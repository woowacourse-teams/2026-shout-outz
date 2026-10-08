import { cleanup, render, screen, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ModalProvider } from '@/components/ModalProvider';
import { createMemoryHistory, createRouter, RouterContextProvider } from '@tanstack/react-router';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { FeedList } from '@/components/feeds/FeedList';
import { FeedMenu } from '@/components/feeds/FeedMenu';
import { PopularFeedList } from '@/components/feeds/PopularFeedList';
import { Comments } from '@/components/feed-comments/Comments';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { createFeedHandlers, mockFeeds } from '@/mocks/handlers';
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

test('피드 목록에는 수정과 삭제 메뉴를 표시하지 않는다', async () => {
  show(<FeedList sort="LATEST" />);
  await screen.findAllByRole('article');
  expect(screen.queryByRole('button', { name: '피드 메뉴' })).not.toBeInTheDocument();
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
    history: createMemoryHistory({ initialEntries: ['/community'] }),
  });
  const wrap = (content: ReactNode) => (
    <QueryClientProvider client={client}>
      <ModalProvider>
        <RouterContextProvider router={router}>
          <AsyncBoundary>{content}</AsyncBoundary>
        </RouterContextProvider>
      </ModalProvider>
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
test('피드 목록 카드는 전체가 상세 페이지로 이동하고 반응 수를 표시한다', async () => {
  const feed = mockFeeds[0]!;
  const detailPath = `/community/${feed.feedId}`;
  show(<FeedList sort="LATEST" />);
  const first = (await screen.findAllByRole('article'))[0]!;
  expect(within(first).getByRole('link', { name: feed.title })).toHaveAttribute('href', detailPath);
  expect(within(first).getByLabelText('좋아요 12개')).toBeInTheDocument();
  expect(within(first).getByLabelText('댓글 1개')).toBeInTheDocument();
  expect(within(first).queryByRole('button', { name: /좋아요|댓글|공유/ })).not.toBeInTheDocument();
});
test('인기 피드는 목록 카드와 같은 링크와 반응 정보를 보여준다', async () => {
  // 목 핸들러는 인기순을 최신순의 역순으로 준다.
  const feed = mockFeeds.at(-1)!;
  show(<PopularFeedList />);
  const firstItem = within(await screen.findByRole('list')).getAllByRole('listitem')[0]!;

  const [detailLink] = within(firstItem).getAllByRole('link');
  expect(within(firstItem).getAllByRole('link')).toHaveLength(1);
  expect(detailLink).toHaveAttribute('href', `/community/${feed.feedId}`);
  expect(firstItem).toHaveTextContent(feed.title);
  expect(firstItem).toHaveTextContent(feed.content);
  expect(within(firstItem).getByLabelText('궁금해요 12개')).toBeInTheDocument();
  expect(within(firstItem).getByLabelText('답변 0개')).toBeInTheDocument();
  expect(within(firstItem).queryByRole('button', { name: '피드 메뉴' })).not.toBeInTheDocument();
});
test('카드는 첫 이미지와 남은 장수만 보여준다', async () => {
  server.use(
    http.get('/api/v1/feeds', () =>
      HttpResponse.json({
        status: 'success',
        data: [
          {
            ...mockFeeds[0]!,
            content: '## 회고\n\n**캐시** 무효화를 `Redis`로 풀었다.',
            media: [1, 2, 3].map((order) => ({
              mediaId: order,
              displayOrder: order,
              url: `https://cdn.test/${order}.png`,
            })),
          },
        ],
        meta: { hasNext: false, nextCursor: null },
      }),
    ),
  );
  show(<FeedList sort="LATEST" />);
  const card = await screen.findByRole('article');

  expect(card).toHaveTextContent('캐시 무효화를 Redis로 풀었다.');
  expect(within(card).getAllByRole('img', { name: '피드 첨부 이미지' })).toHaveLength(1);
  expect(card).toHaveTextContent('이미지 +2장 더 있음');
});
test('카드는 본문의 코드 블록을 빼고 보여준다', async () => {
  server.use(
    http.get('/api/v1/feeds', () =>
      HttpResponse.json({
        status: 'success',
        data: [
          { ...mockFeeds[0]!, content: '설정은 이렇게 했다.\n\n```ts\nconst retry = 3;\n```' },
        ],
        meta: { hasNext: false, nextCursor: null },
      }),
    ),
  );
  show(<FeedList sort="LATEST" />);
  const card = await screen.findByRole('article');

  expect(card).toHaveTextContent('설정은 이렇게 했다.');
  expect(card).not.toHaveTextContent('const retry = 3;');
});
test('본문의 URL은 카드에서 별도 링크로 추출하지 않는다', async () => {
  show(<FeedList sort="LATEST" />);
  const first = (await screen.findAllByRole('article'))[0]!;
  expect(within(first).getAllByRole('link')).toHaveLength(1);
});
test('댓글 정렬 선택 없이 오래된 순으로 조회한다', async () => {
  let requestedSort: string | null = null;
  server.use(
    http.get('*/api/v1/feeds/:feedId/comments', ({ request }) => {
      requestedSort = new URL(request.url).searchParams.get('sort');
      return HttpResponse.json({
        status: 'success',
        data: [
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
        ],
        meta: { hasNext: false, nextCursor: null },
      });
    }),
  );
  show(<Comments feedId={1} />);
  await screen.findByText('새 댓글');

  expect(requestedSort).toBe('OLDEST');
  expect(screen.queryByRole('combobox', { name: '댓글 정렬' })).not.toBeInTheDocument();
  expect(screen.getAllByRole('listitem').map((item) => item.textContent)).toEqual([
    expect.stringContaining('이전 댓글'),
    expect.stringContaining('새 댓글'),
  ]);
});
test('답글을 부모 댓글 아래에 표시하고 새 답글의 parentId를 전달한다', async () => {
  const user = userEvent.setup();
  let postedBody: unknown;
  server.use(
    http.post('*/api/v1/feeds/:feedId/comments', async ({ request }) => {
      postedBody = await request.json();
      return HttpResponse.json({ status: 'success', data: { id: 101 } }, { status: 201 });
    }),
  );
  show(<Comments feedId={3} />);
  const parent = (await screen.findByText('이 댓글은 익명으로 작성했습니다.')).closest('li')!;
  expect(within(parent).queryByText('작성자')).not.toBeInTheDocument();
  expect(within(parent).getByRole('list', { name: '답글 목록' })).toHaveTextContent(
    '저도 같은 경험이 있어요.',
  );
  const replies = within(parent).getByRole('list', { name: '답글 목록' });
  expect(replies).toHaveClass('bg-gray-50/70', 'border-blue-100');
  const replyButton = await within(parent).findByRole('button', { name: '답글 달기' });
  expect(replyButton.parentElement).toContainElement(
    within(parent).getAllByRole('button', { name: '공감' })[0]!,
  );
  await user.click(replyButton);
  await user.type(within(parent).getByRole('textbox', { name: '답글 내용' }), '새 답글');
  await user.click(within(parent).getByRole('button', { name: '답글 등록' }));
  await screen.findByText('답글을 저장했습니다.');
  expect(postedBody).toEqual({ content: '새 답글', parentId: 31, isAnonymous: false });
});
test('익명 댓글은 본인에게 전체 소속을, 타인에게 수료 여부만 표시한다', async () => {
  server.use(
    http.get('*/api/v1/feeds/:feedId/comments', () =>
      HttpResponse.json({
        status: 'success',
        data: [
          {
            id: 101,
            content: '본인 익명 댓글',
            author: {
              userId: 1,
              handle: 'woojin',
              displayName: '개발용 사용자',
              userType: 'WOOWACOURSE_CREW',
              track: 'BACKEND',
              cohort: 8,
              isCurrent: true,
              avatarUrl: null,
            },
            parentId: null,
            isAnonymous: true,
            createdAt: '2026-09-14T00:00:00Z',
            updatedAt: '2026-09-14T00:00:00Z',
            editable: true,
            edited: false,
            deleted: false,
          },
          {
            id: 102,
            content: '다른 사람 익명 댓글',
            author: {
              userId: null,
              handle: null,
              displayName: null,
              userType: 'WOOWACOURSE_CREW',
              track: null,
              cohort: null,
              isCurrent: false,
              avatarUrl: null,
            },
            parentId: null,
            isAnonymous: true,
            createdAt: '2026-09-14T01:00:00Z',
            updatedAt: '2026-09-14T01:00:00Z',
            editable: false,
            edited: false,
            deleted: false,
          },
        ],
        meta: { hasNext: false, nextCursor: null },
      }),
    ),
  );
  show(<Comments feedId={1} feedAuthorId={1} />);

  const own = (await screen.findByText('본인 익명 댓글')).closest('li')!;
  expect(within(own).getByRole('link', { name: '개발용 사용자 프로필 보기' })).toBeInTheDocument();
  expect(within(own).getByText('익명으로 작성한 글입니다')).toBeInTheDocument();
  expect(
    within(own).queryByRole('img', { name: '우아한테크코스 소속 인증' }),
  ).not.toBeInTheDocument();
  expect(within(own).getByText('작성자')).toBeInTheDocument();
  expect(within(own).queryByText('백엔드 크루')).not.toBeInTheDocument();
  expect(within(own).getByText('8기 백엔드 크루')).toBeInTheDocument();
  expect(within(own).queryByText('크루')).not.toBeInTheDocument();

  const other = screen.getByText('다른 사람 익명 댓글').closest('li')!;
  expect(within(other).getByText('익명')).toBeInTheDocument();
  expect(within(other).queryByText('익', { exact: true })).not.toBeInTheDocument();
  expect(within(other).queryByRole('link', { name: /프로필 보기/ })).not.toBeInTheDocument();
  expect(
    within(other).queryByRole('img', { name: '우아한테크코스 소속 인증' }),
  ).not.toBeInTheDocument();
  expect(within(other).getByText('수료생')).toBeInTheDocument();
  expect(within(other).queryByText('작성자')).not.toBeInTheDocument();
  expect(within(other).queryByText('크루')).not.toBeInTheDocument();
});
test('기수 숫자가 내려온 댓글 작성자는 기수를 표시한다', async () => {
  show(<Comments feedId={1} />);

  const comment = (await screen.findByText('경험을 공유해 주셔서 감사합니다!')).closest('li')!;
  expect(within(comment).getByText('8기 백엔드 크루')).toBeInTheDocument();
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
test('비로그인 상태는 익명 댓글에도 작성 및 관리 메뉴를 표시하지 않는다', async () => {
  server.use(
    http.get('*/api/v1/auth/session', () =>
      HttpResponse.json({
        status: 'success',
        data: { status: 'UNAUTHENTICATED', userId: null, role: null, csrfToken: 'token' },
      }),
    ),
  );
  show(<Comments feedId={3} />);
  await screen.findByText('이 댓글은 익명으로 작성했습니다.');
  expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: '삭제' })).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: '수정' })).not.toBeInTheDocument();
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

test('답변 대기순을 서버에 전달하고 본인 익명 질문을 표시한다', async () => {
  let requestedSort: string | null = null;
  let requestedType: string | null = null;
  server.use(
    http.get('*/api/v1/feeds', ({ request }) => {
      requestedSort = new URL(request.url).searchParams.get('sort');
      requestedType = new URL(request.url).searchParams.get('type');
      return HttpResponse.json({
        status: 'success',
        data: [{ ...mockFeeds[0]!, feedType: 'QUESTION', isAnonymous: true, commentCount: 0 }],
        meta: { hasNext: false, nextCursor: null },
      });
    }),
  );
  show(<FeedList sort="WAITING" />);
  const card = await screen.findByRole('article');
  expect(requestedSort).toBe('WAITING');
  expect(requestedType).toBe('QUESTION');
  expect(within(card).getByText('답변을 기다리고 있어요')).toBeInTheDocument();
  expect(within(card).getByText('정우진')).toBeInTheDocument();
  expect(within(card).getByText('익명으로 작성한 글입니다')).toBeInTheDocument();
  expect(within(card).queryByRole('link', { name: /프로필 보기/ })).not.toBeInTheDocument();
});

test('익명 댓글 선택을 작성 요청에 전달한다', async () => {
  let body: unknown;
  server.use(
    http.post('*/api/v1/feeds/:feedId/comments', async ({ request }) => {
      body = await request.json();
      return HttpResponse.json({ status: 'success', data: { id: 123 } });
    }),
  );
  const user = userEvent.setup();
  show(<Comments feedId={1} />);
  await user.type(
    await screen.findByRole('textbox', { name: '댓글 남기기' }),
    '익명으로 경험 공유',
  );
  await user.click(screen.getByRole('checkbox', { name: '익명으로 남기기' }));
  await user.click(screen.getByRole('button', { name: '댓글 작성' }));
  await screen.findByText('댓글을 저장했습니다.');
  expect(body).toEqual({ content: '익명으로 경험 공유', isAnonymous: true });
  expect(screen.getByRole('checkbox', { name: '익명으로 남기기' })).not.toBeChecked();
});

test.each(['LATEST', 'POPULAR', 'WAITING'] as const)('질문 %s 빈 목록을 안내한다', async (sort) => {
  server.use(
    http.get('*/api/v1/feeds', () =>
      HttpResponse.json({
        status: 'success',
        data: [],
        meta: { hasNext: false, nextCursor: null },
      }),
    ),
  );
  show(<FeedList sort={sort} feedType="QUESTION" />);
  expect(
    await screen.findByText(
      sort === 'WAITING' ? '답변을 기다리는 질문이 없어요!' : '아직 등록된 질문이 없습니다.',
    ),
  ).toBeInTheDocument();
});
