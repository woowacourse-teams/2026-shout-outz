import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

type User = ReturnType<typeof userEvent.setup>;
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ModalProvider } from '@/components/ModalProvider';
import {
  createMemoryHistory,
  createRouter,
  RouterContextProvider,
  RouterProvider,
} from '@tanstack/react-router';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { FeedEditorPage } from '@/pages/FeedEditorPage';
import { createFeedHandlers, mockFeeds } from '@/mocks/handlers';
import { routeTree } from '@/routeTree.gen';
import { fetchFeed, fetchFeeds } from '@/apis/feed';

const server = setupServer();
let client: QueryClient;
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
beforeEach(() => {
  server.use(...createFeedHandlers());
  jest.spyOn(window, 'scrollTo').mockImplementation(() => {});
});
afterEach(() => {
  cleanup();
  client.clear();
  server.resetHandlers();
  jest.restoreAllMocks();
});
afterAll(() => server.close());

function show(feedId?: number) {
  client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  const router = createRouter({
    routeTree,
    history: createMemoryHistory({ initialEntries: ['/feeds/new'] }),
  });
  const onSaved = jest.fn();
  const onCancel = jest.fn();
  render(
    <QueryClientProvider client={client}>
      <ModalProvider>
        <RouterContextProvider router={router}>
          <FeedEditorPage feedId={feedId} onCancel={onCancel} onSaved={onSaved} />
        </RouterContextProvider>
      </ModalProvider>
    </QueryClientProvider>,
  );
  return { onSaved, onCancel };
}

/** 제목은 작성·수정 모두 필수라, 본문만 보는 테스트에서도 한 번은 채워야 제출이 열린다. */
const fillTitle = async (user: User, value = '제목') => {
  const input = await screen.findByRole('textbox', { name: '제목' });
  await user.clear(input);
  await user.type(input, value);
};

test('실제 작성자를 표시하고 등록한 피드를 상세와 목록에서 조회할 수 있다', async () => {
  const user = userEvent.setup();
  const { onSaved } = show();
  const input = await screen.findByRole('textbox', { name: '피드 내용' });
  await user.click(screen.getByRole('combobox', { name: '카테고리' }));
  await user.click(screen.getByRole('option', { name: '백엔드' }));
  expect(screen.getByText('정우진 · 6기 백엔드')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: '피드 등록하기' })).toBeDisabled();
  await fillTitle(user, '새 피드 제목');
  await user.type(input, '새로운 기술 이야기');
  await user.click(screen.getByRole('button', { name: '피드 등록하기' }));
  await screen.findByRole('button', { name: '피드 등록하기' });
  expect(onSaved).toHaveBeenCalledWith(100);
  expect((await fetchFeed(100)).content).toBe('새로운 기술 이야기');
  expect((await fetchFeeds({ sort: 'LATEST', size: 20 })).data[0]?.feedId).toBe(100);
});

test('실패하면 서버 메시지와 입력 내용을 유지한다', async () => {
  server.use(
    http.post('*/api/v1/feeds', () =>
      HttpResponse.json(
        {
          status: 'error',
          code: 'POST_WRITER_TYPE_FORBIDDEN',
          message: '활성 크루 또는 코치만 작성할 수 있습니다.',
        },
        { status: 403 },
      ),
    ),
  );
  const user = userEvent.setup();
  const { onSaved } = show();
  const input = await screen.findByRole('textbox', { name: '피드 내용' });
  await user.click(screen.getByRole('combobox', { name: '카테고리' }));
  await user.click(screen.getByRole('option', { name: '백엔드' }));
  await fillTitle(user);
  await user.type(input, '보존할 내용');
  await user.click(screen.getByRole('button', { name: '피드 등록하기' }));
  expect(await screen.findByRole('alert')).toHaveTextContent(
    '활성 크루 또는 코치만 작성할 수 있습니다.',
  );
  expect(input).toHaveValue('보존할 내용');
  expect(onSaved).not.toHaveBeenCalled();
});

test('Unicode 500자까지 허용하고 초과하면 등록을 막는다', async () => {
  const user = userEvent.setup();
  show();
  const input = await screen.findByRole('textbox', { name: '피드 내용' });
  await user.click(screen.getByRole('combobox', { name: '카테고리' }));
  await user.click(screen.getByRole('option', { name: '백엔드' }));
  await fillTitle(user);
  await user.click(input);
  await user.paste('😀'.repeat(500));
  expect(screen.getByRole('button', { name: '피드 등록하기' })).toBeEnabled();
  await user.paste('가');
  expect(screen.getByRole('alert')).toHaveTextContent('본문은 500자 이하로 입력해 주세요.');
  expect(screen.getByRole('button', { name: '피드 등록하기' })).toBeDisabled();
});

test('취소하면 취소 콜백을 호출한다', async () => {
  const user = userEvent.setup();
  const { onCancel } = show();
  await user.click(await screen.findByRole('button', { name: '취소' }));
  expect(onCancel).toHaveBeenCalledTimes(1);
});

test('비로그인 사용자는 폼 대신 로그인 안내를 본다', async () => {
  server.use(
    http.get('*/api/v1/auth/session', () =>
      HttpResponse.json({
        status: 'success',
        data: { status: 'UNAUTHENTICATED', userId: null, role: null, csrfToken: 'token' },
      }),
    ),
  );
  show();
  expect(await screen.findByRole('link', { name: 'GitHub 로그인' })).toHaveAttribute(
    'href',
    'http://localhost/oauth2/authorization/github',
  );
  expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
});

test('카테고리가 없으면 등록을 막고 EVENT는 선택지에서 제외한다', async () => {
  const user = userEvent.setup();
  show();
  await fillTitle(user);
  await user.type(await screen.findByRole('textbox', { name: '피드 내용' }), '내용');
  expect(screen.getByRole('button', { name: '피드 등록하기' })).toBeDisabled();
  await user.click(screen.getByRole('combobox', { name: '카테고리' }));
  expect(screen.queryByRole('option', { name: '테코드톡' })).not.toBeInTheDocument();
  await user.click(screen.getByRole('option', { name: '백엔드' }));
  expect(screen.getByRole('button', { name: '피드 등록하기' })).toBeEnabled();
});

test('등록 중에는 중복 요청을 보내지 않는다', async () => {
  let finish!: () => void;
  const pending = new Promise<void>((resolve) => {
    finish = resolve;
  });
  let requests = 0;
  server.use(
    http.post('*/api/v1/feeds', async () => {
      requests++;
      await pending;
      return HttpResponse.json(
        { status: 'error', code: 'VALIDATION_ERROR', message: '다시 시도해 주세요.' },
        { status: 400 },
      );
    }),
  );
  const user = userEvent.setup();
  show();
  await fillTitle(user);
  await user.type(await screen.findByRole('textbox', { name: '피드 내용' }), '내용');
  await user.click(screen.getByRole('combobox', { name: '카테고리' }));
  await user.click(screen.getByRole('option', { name: '백엔드' }));
  try {
    await user.dblClick(screen.getByRole('button', { name: '피드 등록하기' }));
    await waitFor(() => expect(requests).toBe(1));
    expect(screen.getByRole('button', { name: '저장 중…' })).toBeDisabled();
  } finally {
    finish();
  }
  await screen.findByRole('alert');
});

test('목록에서 작성 페이지 진입 후 등록하면 생성한 상세 페이지로 이동한다', async () => {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: () => ({ matches: false, addEventListener() {}, removeEventListener() {} }),
  });
  client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  const router = createRouter({
    routeTree,
    history: createMemoryHistory({ initialEntries: ['/feeds'] }),
  });
  render(
    <QueryClientProvider client={client}>
      <ModalProvider>
        <RouterProvider router={router} />
      </ModalProvider>
    </QueryClientProvider>,
  );
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: '글쓰기' }));
  await fillTitle(user);
  await user.type(
    await screen.findByRole('textbox', { name: '피드 내용' }),
    '등록 후 상세에서 확인할 내용',
  );
  expect(router.state.location.pathname).toBe('/feeds/new');
  await user.click(screen.getByRole('combobox', { name: '카테고리' }));
  await user.click(screen.getByRole('option', { name: '백엔드' }));
  await user.click(screen.getByRole('button', { name: '피드 등록하기' }));
  await screen.findByText('등록 후 상세에서 확인할 내용');
  expect(router.state.location.pathname).toBe('/feeds/100');
});

test('수정은 기존 본문과 카테고리를 채우고 저장 결과를 상세에 반영한다', async () => {
  const user = userEvent.setup();
  const { onSaved } = show(1);
  const input = await screen.findByRole('textbox', { name: '피드 내용' });
  expect(input).toHaveValue(mockFeeds[0]!.content);
  expect(screen.getByRole('combobox', { name: '카테고리' })).toHaveTextContent('백엔드');
  await user.clear(input);
  await user.type(input, '수정한 본문');
  await user.click(screen.getByRole('button', { name: '수정 완료' }));
  await waitFor(() => expect(onSaved).toHaveBeenCalledWith(1));
  expect((await fetchFeed(1)).content).toBe('수정한 본문');
  expect(client.getQueryData(['feed', 1])).toMatchObject({ content: '수정한 본문' });
});

test('수정 실패 시 본문을 보존하고 API 메시지를 표시한다', async () => {
  server.use(
    http.put('*/api/v1/feeds/:feedId', () =>
      HttpResponse.json(
        { status: 'error', code: 'VALIDATION_ERROR', message: '수정 요청을 확인해 주세요.' },
        { status: 400 },
      ),
    ),
  );
  const user = userEvent.setup();
  const { onSaved } = show(1);
  const input = await screen.findByRole('textbox', { name: '피드 내용' });
  await user.clear(input);
  await user.type(input, '보존할 수정 내용');
  await user.click(screen.getByRole('button', { name: '수정 완료' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('수정 요청을 확인해 주세요.');
  expect(input).toHaveValue('보존할 수정 내용');
  expect(onSaved).not.toHaveBeenCalled();
});

test('다른 작성자의 피드는 수정 폼을 표시하지 않는다', async () => {
  show(2);
  expect(await screen.findByRole('alert')).toHaveTextContent(
    '본인이 작성한 피드만 수정할 수 있습니다.',
  );
  expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
});

test('수정 시 기존 이벤트 카테고리와 미디어 연결을 보존한다', async () => {
  const original = {
    ...mockFeeds[0]!,
    categories: [
      ...mockFeeds[0]!.categories,
      { categoryId: 3, slug: 'event', displayName: '이벤트', type: 'EVENT' },
    ],
    media: [
      { displayOrder: 1, mediaId: 32, url: 'https://cdn.example.com/media/32' },
      { displayOrder: 0, mediaId: 31, url: 'https://cdn.example.com/media/31' },
    ],
  };
  let body: unknown;
  server.use(
    http.get('*/api/v1/feeds/1', () => HttpResponse.json({ status: 'success', data: original })),
    http.put('*/api/v1/feeds/1', async ({ request }) => {
      body = await request.json();
      return HttpResponse.json({ status: 'success', data: original });
    }),
  );
  const user = userEvent.setup();
  const { onSaved } = show(1);
  await user.click(await screen.findByRole('button', { name: '수정 완료' }));
  await waitFor(() => expect(onSaved).toHaveBeenCalledWith(1));
  expect(body).toEqual({
    title: original.title,
    content: original.content,
    categoryIds: [1, 3],
    mediaIds: [31, 32],
  });
});

test('본인 글 메뉴에서 수정하고 저장하면 상세 페이지로 돌아간다', async () => {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: () => ({ matches: false, addEventListener() {}, removeEventListener() {} }),
  });
  client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  const router = createRouter({
    routeTree,
    history: createMemoryHistory({ initialEntries: ['/feeds'] }),
  });
  render(
    <QueryClientProvider client={client}>
      <ModalProvider>
        <RouterProvider router={router} />
      </ModalProvider>
    </QueryClientProvider>,
  );
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: '피드 메뉴' }));
  await user.click(screen.getByRole('menuitem', { name: '수정' }));
  const input = await screen.findByRole('textbox', { name: '피드 내용' });
  expect(router.state.location.pathname).toBe('/feeds/1/edit');
  await user.clear(input);
  await user.type(input, '수정 페이지에서 저장한 내용');
  await user.click(screen.getByRole('button', { name: '수정 완료' }));
  await screen.findByText('수정 페이지에서 저장한 내용');
  expect(router.state.location.pathname).toBe('/feeds/1');
});

test('삭제 확인 후 피드를 목록에서 제거하고 상세 조회도 실패한다', async () => {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: () => ({ matches: false, addEventListener() {}, removeEventListener() {} }),
  });
  client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  const router = createRouter({
    routeTree,
    history: createMemoryHistory({ initialEntries: ['/feeds/1'] }),
  });
  render(
    <QueryClientProvider client={client}>
      <ModalProvider>
        <RouterProvider router={router} />
      </ModalProvider>
    </QueryClientProvider>,
  );
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: '피드 메뉴' }));
  await user.click(screen.getByRole('menuitem', { name: '삭제' }));
  expect(screen.getByRole('group', { name: '피드 삭제 확인' })).toBeInTheDocument();
  expect(router.state.location.pathname).toBe('/feeds/1');
  await user.click(screen.getByRole('button', { name: '삭제 확인' }));
  await waitFor(() => expect(router.state.location.pathname).toBe('/feeds'));
  expect(
    (await fetchFeeds({ sort: 'LATEST', size: 20 })).data.some((feed) => feed.feedId === 1),
  ).toBe(false);
  await expect(fetchFeed(1)).rejects.toThrow();
});

test('삭제 실패 시 서버 메시지를 표시하고 상세에 머문다', async () => {
  server.use(
    http.delete('*/api/v1/feeds/:feedId', () =>
      HttpResponse.json(
        {
          status: 'error',
          code: 'POST_AUTHOR_FORBIDDEN',
          message: '본인이 작성한 피드만 삭제할 수 있습니다.',
        },
        { status: 403 },
      ),
    ),
  );
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: () => ({ matches: false, addEventListener() {}, removeEventListener() {} }),
  });
  client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  const router = createRouter({
    routeTree,
    history: createMemoryHistory({ initialEntries: ['/feeds/1'] }),
  });
  render(
    <QueryClientProvider client={client}>
      <ModalProvider>
        <RouterProvider router={router} />
      </ModalProvider>
    </QueryClientProvider>,
  );
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: '피드 메뉴' }));
  await user.click(screen.getByRole('menuitem', { name: '삭제' }));
  await user.click(screen.getByRole('button', { name: '삭제 확인' }));
  expect(await screen.findByRole('alert')).toHaveTextContent(
    '본인이 작성한 피드만 삭제할 수 있습니다.',
  );
  expect(router.state.location.pathname).toBe('/feeds/1');
});

test('제목이 비면 등록을 막는다', async () => {
  const user = userEvent.setup();
  show();

  await user.type(await screen.findByRole('textbox', { name: '피드 내용' }), '내용');
  await user.click(screen.getByRole('combobox', { name: '카테고리' }));
  await user.click(screen.getByRole('option', { name: '백엔드' }));
  expect(screen.getByRole('button', { name: '피드 등록하기' })).toBeDisabled();

  await fillTitle(user);

  expect(screen.getByRole('button', { name: '피드 등록하기' })).toBeEnabled();
});

test('제목은 Unicode 100자까지 허용하고 초과하면 등록을 막는다', async () => {
  const user = userEvent.setup();
  show();

  await user.type(await screen.findByRole('textbox', { name: '피드 내용' }), '내용');
  await user.click(screen.getByRole('combobox', { name: '카테고리' }));
  await user.click(screen.getByRole('option', { name: '백엔드' }));

  const title = await screen.findByRole('textbox', { name: '제목' });
  await user.click(title);
  await user.paste('😀'.repeat(100));
  expect(screen.getByRole('button', { name: '피드 등록하기' })).toBeEnabled();

  await user.paste('가');

  expect(screen.getByRole('alert')).toHaveTextContent('제목은 100자 이하로 입력해 주세요.');
  expect(screen.getByRole('button', { name: '피드 등록하기' })).toBeDisabled();
});

test('제목 앞뒤 공백은 잘라서 보낸다', async () => {
  let body: unknown;
  server.use(
    http.post('*/api/v1/feeds', async ({ request }) => {
      body = await request.json();
      return HttpResponse.json({ status: 'success', data: mockFeeds[0] }, { status: 201 });
    }),
  );
  const user = userEvent.setup();
  show();

  await fillTitle(user, '  제목에 공백  ');
  await user.type(await screen.findByRole('textbox', { name: '피드 내용' }), '내용');
  await user.click(screen.getByRole('combobox', { name: '카테고리' }));
  await user.click(screen.getByRole('option', { name: '백엔드' }));
  await user.click(screen.getByRole('button', { name: '피드 등록하기' }));

  await waitFor(() => expect(body).toHaveProperty('title', '제목에 공백'));
});

test('수정 화면은 기존 제목을 채워 보여준다', async () => {
  const user = userEvent.setup();
  show(1);

  expect(await screen.findByRole('textbox', { name: '제목' })).toHaveValue(mockFeeds[0]!.title);

  await user.clear(screen.getByRole('textbox', { name: '제목' }));

  expect(screen.getByRole('button', { name: '수정 완료' })).toBeDisabled();
});

test('제목·본문·카테고리가 하나라도 비면 등록 버튼이 잠겨 있다', async () => {
  const user = userEvent.setup();
  show();

  const submit = await screen.findByRole('button', { name: '피드 등록하기' });
  expect(submit).toBeDisabled();

  await fillTitle(user);
  expect(submit).toBeDisabled();

  await user.type(screen.getByRole('textbox', { name: '피드 내용' }), '내용');
  expect(submit).toBeDisabled();

  await user.click(screen.getByRole('combobox', { name: '카테고리' }));
  await user.click(screen.getByRole('option', { name: '백엔드' }));
  expect(submit).toBeEnabled();

  // 하나라도 지우면 다시 잠긴다.
  await user.clear(screen.getByRole('textbox', { name: '제목' }));
  expect(submit).toBeDisabled();
});
