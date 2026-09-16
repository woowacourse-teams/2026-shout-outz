import { cleanup, render, screen, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMemoryHistory, createRouter, RouterContextProvider } from '@tanstack/react-router';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { FeedList } from '@/components/feeds/FeedList';
import { FeedMenu } from '@/components/feeds/FeedMenu';
import { Comments } from '@/components/feed-comments/Comments';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { createFeedHandlers } from '@/mocks/handlers';
import { routeTree } from '@/routeTree.gen';
import type { ReactNode } from 'react';

const server = setupServer();
let client: QueryClient;
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
beforeEach(() => server.use(...createFeedHandlers()));
afterEach(() => {
  cleanup();
  client.clear();
  server.resetHandlers();
});
afterAll(() => server.close());

test('본인 글에만 수정과 삭제 메뉴를 표시한다', async () => {
  const user = userEvent.setup();
  show(<FeedList sort="LATEST" />);
  const trigger = await screen.findByRole('button', { name: '피드 메뉴' });
  expect(screen.getAllByRole('button', { name: '피드 메뉴' })).toHaveLength(1);
  expect(
    within(screen.getAllByRole('article')[0]!).getByRole('button', { name: '피드 메뉴' }),
  ).toBe(trigger);
  await user.click(trigger);
  expect(screen.getByRole('menuitem', { name: '수정' })).toBeInTheDocument();
  await user.click(screen.getByRole('menuitem', { name: '삭제' }));
  expect(screen.queryByRole('menu')).not.toBeInTheDocument();
});

test('비로그인 상태에서는 프로필 조회 없이 피드 메뉴를 숨긴다', async () => {
  const profileRequested = jest.fn();
  server.use(
    http.get('*/api/v1/auth/session', () =>
      HttpResponse.json({
        status: 'success',
        data: { status: 'UNAUTHENTICATED', userId: null, role: null, csrfToken: 'token' },
      }),
    ),
    http.get('*/api/v1/users/me/summary', () => {
      profileRequested();
      return HttpResponse.error();
    }),
  );
  show(<FeedMenu feedId={1} authorHandle="crew0" />);
  await waitFor(() => expect(screen.queryByRole('status')).not.toBeInTheDocument());
  expect(screen.queryByRole('button', { name: '피드 메뉴' })).not.toBeInTheDocument();
  expect(profileRequested).not.toHaveBeenCalled();
});

function show(children: ReactNode) {
  client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  const router = createRouter({
    routeTree,
    history: createMemoryHistory({ initialEntries: ['/feeds'] }),
  });
  const wrap = (content: ReactNode) => (
    <QueryClientProvider client={client}>
      <RouterContextProvider router={router}>
        <AsyncBoundary>{content}</AsyncBoundary>
      </RouterContextProvider>
    </QueryClientProvider>
  );
  const view = render(wrap(children));
  return { ...view, rerenderWith: (content: ReactNode) => view.rerender(wrap(content)) };
}
test('피드 다음 페이지를 추가하고 마지막 페이지에서 멈춘다', async () => {
  const user = userEvent.setup();
  show(<FeedList sort="LATEST" />);
  await screen.findByRole('button', { name: '피드 더 보기' });
  expect(screen.getAllByRole('article')).toHaveLength(3);
  await user.click(screen.getByRole('button', { name: '피드 더 보기' }));
  await screen.findAllByRole('article');
  await screen.findByText(
    (_, element) =>
      element?.tagName === 'TIME' &&
      element.getAttribute('datetime') === '2026-09-14T04:00:00.000Z',
  );
  expect(screen.getAllByRole('article')).toHaveLength(6);
  expect(screen.queryByRole('button', { name: '피드 더 보기' })).not.toBeInTheDocument();
});
test('빈 피드를 안내한다', async () => {
  server.use(
    http.get('*/api/v1/feeds', () =>
      HttpResponse.json({
        status: 'success',
        data: [],
        meta: { hasNext: false, nextCursor: null },
      }),
    ),
  );
  show(<FeedList sort="LATEST" />);
  expect(await screen.findByText('아직 등록된 피드가 없습니다.')).toBeInTheDocument();
});
test('피드 링크와 공유 주소가 상세 페이지를 가리킨다', async () => {
  const user = userEvent.setup();
  show(<FeedList sort="LATEST" />);
  const first = (await screen.findAllByRole('article'))[0]!;
  expect(within(first).getByRole('link', { name: /정우진/ })).toHaveAttribute('href', '/feeds/1');
  await user.click(within(first).getByRole('button', { name: '공유' }));
  expect(await navigator.clipboard.readText()).toBe(
    new URL('/feeds/1', window.location.origin).href,
  );
});
test('댓글 정렬 선택 없이 최신순으로 조회한다', async () => {
  let requestedSort: string | null = null;
  server.use(
    http.get('*/api/v1/feeds/:feedId/comments', ({ request }) => {
      requestedSort = new URL(request.url).searchParams.get('sort');
      return HttpResponse.json({
        status: 'success',
        data: [
          {
            id: 2,
            content: '새 댓글',
            author: { userId: 2, displayName: '새 작성자', avatarImageId: null },
            parentId: null,
            createdAt: '2026-09-15T02:00:00Z',
            updatedAt: '2026-09-15T02:00:00Z',
            editable: false,
            edited: false,
            deleted: false,
          },
          {
            id: 1,
            content: '이전 댓글',
            author: { userId: 3, displayName: '이전 작성자', avatarImageId: null },
            parentId: null,
            createdAt: '2026-09-14T02:00:00Z',
            updatedAt: '2026-09-14T02:00:00Z',
            editable: false,
            edited: false,
            deleted: false,
          },
        ],
        meta: { hasNext: false, nextCursor: null },
      });
    }),
  );
  show(<Comments feedId={1} />);
  await screen.findByText('새 댓글');

  expect(requestedSort).toBe('LATEST');
  expect(screen.queryByRole('combobox', { name: '댓글 정렬' })).not.toBeInTheDocument();
  expect(screen.getAllByRole('listitem').map((item) => item.textContent)).toEqual([
    expect.stringContaining('새 댓글'),
    expect.stringContaining('이전 댓글'),
  ]);
});
test('댓글 작성, 수정, 삭제가 조회 결과에 반영된다', async () => {
  const user = userEvent.setup();
  show(<Comments feedId={1} />);
  const input = await screen.findByRole('textbox', { name: '댓글 남기기' });
  await user.type(input, '새 댓글입니다');
  await user.click(screen.getByRole('button', { name: '댓글 작성' }));
  const item = (await screen.findByText('새 댓글입니다')).closest('li')!;
  expect(input).toHaveValue('');
  await user.click(within(item).getByRole('button', { name: '수정' }));
  const edit = within(item).getByRole('textbox', { name: '댓글 수정 내용' });
  await user.clear(edit);
  await user.type(edit, '수정한 댓글');
  await user.click(within(item).getByRole('button', { name: '수정 저장' }));
  await screen.findByText('수정한 댓글');
  await user.click(within(item).getByRole('button', { name: '삭제' }));
  await user.click(within(item).getByRole('button', { name: '삭제 확인' }));
  await screen.findByText('댓글을 삭제했습니다.');
  expect(screen.queryByText('수정한 댓글')).not.toBeInTheDocument();
  expect(screen.getByText('삭제된 댓글입니다.')).toBeInTheDocument();
});
test('작성 실패 시 입력을 보존한다', async () => {
  server.use(
    http.post('*/api/v1/feeds/:feedId/comments', () =>
      HttpResponse.json(
        { status: 'error', code: 'VALIDATION_ERROR', message: '댓글 내용을 확인해 주세요.' },
        { status: 400 },
      ),
    ),
  );
  const user = userEvent.setup();
  show(<Comments feedId={1} />);
  const input = await screen.findByRole('textbox', { name: '댓글 남기기' });
  await user.type(input, '보존할 내용');
  await user.click(screen.getByRole('button', { name: '댓글 작성' }));
  expect(await screen.findByText('댓글 내용을 확인해 주세요.')).toBeInTheDocument();
  expect(input).toHaveValue('보존할 내용');
});
test('비로그인 상태는 작성 및 관리 메뉴를 표시하지 않는다', async () => {
  server.use(
    http.get('*/api/v1/auth/session', () =>
      HttpResponse.json({
        status: 'success',
        data: { status: 'UNAUTHENTICATED', userId: null, role: null, csrfToken: 'token' },
      }),
    ),
  );
  show(<Comments feedId={1} />);
  await screen.findByText('경험을 공유해 주셔서 감사합니다!');
  expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: '삭제' })).not.toBeInTheDocument();
});

test('세션 조회가 실패해도 공개 댓글을 읽고 로그인 상태를 재확인할 수 있다', async () => {
  const errors = jest.spyOn(console, 'error').mockImplementation(() => {});
  try {
    server.use(
      http.get('*/api/v1/auth/session', () =>
        HttpResponse.json(
          {
            status: 'error',
            code: 'SESSION_UNAVAILABLE',
            message: '로그인 상태를 확인하지 못했습니다.',
          },
          { status: 503 },
        ),
      ),
    );
    const user = userEvent.setup();
    show(<Comments feedId={1} />);
    expect(
      await screen.findByText('경험을 공유해 주셔서 감사합니다!', {}, { timeout: 5000 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('로그인 상태를 확인하지 못했습니다.');
    expect(screen.queryByRole('textbox', { name: '댓글 남기기' })).not.toBeInTheDocument();
    server.resetHandlers(...createFeedHandlers());
    await user.click(screen.getByRole('button', { name: '로그인 상태 다시 확인' }));
    expect(await screen.findByRole('textbox', { name: '댓글 남기기' })).toBeInTheDocument();
  } finally {
    errors.mockRestore();
  }
});

test('정렬 변경 시 이전 정렬의 페이지를 섞지 않는다', async () => {
  const view = show(<FeedList sort="LATEST" />);
  await screen.findByRole('button', { name: '피드 더 보기' });
  expect(screen.getAllByRole('article')[0]?.querySelector('time')).toHaveAttribute(
    'datetime',
    '2026-09-14T09:00:00.000Z',
  );
  view.rerenderWith(<FeedList sort="POPULAR" />);
  await screen.findByText(
    (_, element) =>
      element?.tagName === 'TIME' &&
      element.getAttribute('datetime') === '2026-09-14T04:00:00.000Z',
  );
  expect(screen.getAllByRole('article')).toHaveLength(3);
  expect(screen.getAllByRole('article')[0]?.querySelector('time')).toHaveAttribute(
    'datetime',
    '2026-09-14T04:00:00.000Z',
  );
});

test('추가 조회 실패 시 기존 피드를 보존하고 재시도한다', async () => {
  const user = userEvent.setup();
  show(<FeedList sort="LATEST" />);
  await screen.findByRole('button', { name: '피드 더 보기' });
  server.use(
    http.get('*/api/v1/feeds', () =>
      HttpResponse.json(
        { status: 'error', code: 'FEED_FETCH_FAILED', message: '피드 조회에 실패했습니다.' },
        { status: 400 },
      ),
    ),
  );
  await user.click(screen.getByRole('button', { name: '피드 더 보기' }));
  await screen.findByRole('button', { name: '추가 피드 다시 시도' });
  expect(screen.getByRole('alert')).toHaveTextContent('피드 조회에 실패했습니다.');
  expect(screen.getAllByRole('article')).toHaveLength(3);
  server.resetHandlers(...createFeedHandlers());
  await user.click(screen.getByRole('button', { name: '추가 피드 다시 시도' }));
  await screen.findByText(
    (_, element) =>
      element?.tagName === 'TIME' &&
      element.getAttribute('datetime') === '2026-09-14T04:00:00.000Z',
  );
  expect(screen.getAllByRole('article')).toHaveLength(6);
});

test('저장 성공 후 갱신 실패를 구분하여 안내한다', async () => {
  const user = userEvent.setup();
  show(<Comments feedId={1} />);
  const input = await screen.findByRole('textbox', { name: '댓글 남기기' });
  const failureHandler = http.get('*/api/v1/feeds/:feedId/comments', () =>
    HttpResponse.json(
      { status: 'error', code: 'COMMENT_FETCH_FAILED', message: '댓글 조회에 실패했습니다.' },
      { status: 400 },
    ),
  );
  server.use(failureHandler);
  await user.type(input, '저장은 성공');
  await user.click(screen.getByRole('button', { name: '댓글 작성' }));
  await screen.findByText('댓글을 저장했습니다.');
  expect(await screen.findByRole('button', { name: '목록 새로고침' })).toBeInTheDocument();
  expect(screen.getByRole('alert')).toHaveTextContent('댓글 조회에 실패했습니다.');
  expect(input).toHaveValue('');
  expect(screen.getByText('경험을 공유해 주셔서 감사합니다!')).toBeInTheDocument();
});

test('초기 조회 오류 경계에서 다시 시도할 수 있다', async () => {
  const errors = jest.spyOn(console, 'error').mockImplementation(() => {});
  try {
    server.use(
      http.get('*/api/v1/feeds', () =>
        HttpResponse.json(
          { status: 'error', code: 'FEED_FETCH_FAILED', message: '피드 조회에 실패했습니다.' },
          { status: 400 },
        ),
      ),
    );
    const user = userEvent.setup();
    show(<FeedList sort="LATEST" />);
    await screen.findByRole('button', { name: '다시 시도' });
    expect(screen.getByRole('alert')).toHaveTextContent('피드 조회에 실패했습니다.');
    server.resetHandlers(...createFeedHandlers());
    await user.click(screen.getByRole('button', { name: '다시 시도' }));
    await screen.findByRole('button', { name: '피드 더 보기' });
    expect(screen.getAllByRole('article')).toHaveLength(3);
  } finally {
    errors.mockRestore();
  }
});

test('서버가 같은 커서를 반복하면 추가 조회를 중단한다', async () => {
  server.use(
    http.get('*/api/v1/feeds', () =>
      HttpResponse.json({
        status: 'success',
        data: [],
        meta: { hasNext: true, nextCursor: 'repeat' },
      }),
    ),
  );
  const user = userEvent.setup();
  show(<FeedList sort="LATEST" />);
  await user.click(await screen.findByRole('button', { name: '피드 더 보기' }));
  await screen.findByText('아직 등록된 피드가 없습니다.');
  await waitFor(() =>
    expect(screen.queryByRole('button', { name: '피드 더 보기' })).not.toBeInTheDocument(),
  );
});
