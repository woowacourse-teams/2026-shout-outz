import { cleanup, render, screen, within } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ModalProvider } from '@/components/ModalProvider';
import { createMemoryHistory, createRouter, RouterContextProvider } from '@tanstack/react-router';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { FeedDetailPage } from '@/pages/FeedDetailPage';
import { createFeedHandlers } from '@/mocks/handlers';
import { routeTree } from '@/routeTree.gen';

const server = setupServer();
let client: QueryClient;

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
beforeEach(() => {
  server.use(
    ...createFeedHandlers(),
    http.get('*/api/v1/notifications/unread-count', () =>
      HttpResponse.json({ status: 'success', data: { unreadCount: 0 } }),
    ),
  );
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: () => ({ matches: false, addEventListener() {}, removeEventListener() {} }),
  });
});
afterEach(() => {
  cleanup();
  client.clear();
  server.resetHandlers();
});
afterAll(() => server.close());

function show(feedId: number) {
  client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  const router = createRouter({
    routeTree,
    history: createMemoryHistory({ initialEntries: [`/community/${feedId}`] }),
  });
  render(
    <QueryClientProvider client={client}>
      <ModalProvider>
        <RouterContextProvider router={router}>
          <FeedDetailPage feedId={feedId} />
        </RouterContextProvider>
      </ModalProvider>
    </QueryClientProvider>,
  );
}

test('상세 URL에서 피드와 댓글을 각각 조회한다', async () => {
  show(1);
  expect(await screen.findByText(/Redis Pub\/Sub으로 WebSocket/)).toBeInTheDocument();
  await screen.findByRole('textbox', { name: '댓글 남기기' });
  expect(screen.getByRole('button', { name: '댓글 작성' })).toHaveTextContent('댓글 등록하기');
  expect(await screen.findByText('경험을 공유해 주셔서 감사합니다!')).toBeInTheDocument();
  expect(screen.getByText('2026.09.14 09:00')).toBeInTheDocument();
  const notice = screen.getByText('익명으로 작성한 글입니다');
  expect(notice.parentElement).toHaveTextContent('정우진');
  const author = screen.getByRole('link', { name: '정우진 프로필 보기' });
  expect(
    within(author).queryByRole('img', { name: '우아한테크코스 소속 인증' }),
  ).not.toBeInTheDocument();
  expect(within(author).getByText('8기 백엔드 크루')).toBeInTheDocument();
  // 리액션 API가 생겨 좋아요를 누를 수 있다. 비활성은 요청이 도는 동안뿐이다.
  expect(screen.getByRole('button', { name: '좋아요' })).toBeEnabled();
});

test('기수 숫자가 내려온 피드 작성자는 기수를 표시한다', async () => {
  show(2);

  const author = await screen.findByRole('link', { name: '김도현 프로필 보기' });
  expect(within(author).getByText('6기 프론트엔드 크루')).toBeInTheDocument();
});

test('질문 피드의 공유 안내를 본문 위에 두고 좋아요 문구를 표시한다', async () => {
  show(3);
  const share = await screen.findByRole('button', { name: '공유' });
  const body = await screen.findByText('프로젝트에서 가장 기억에 남는 트러블슈팅은 무엇인가요?');
  expect(share).toHaveTextContent('친구에게 공유해보세요!');
  expect(share.parentElement).toHaveClass('absolute');
  expect(share.parentElement?.parentElement).toHaveClass('relative');
  expect(share.compareDocumentPosition(body) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  expect(screen.getByRole('button', { name: '저도 궁금해요' })).toHaveTextContent('저도 궁금해요');
  expect(await screen.findByRole('textbox', { name: '답변 남기기' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: '답변 작성' })).toHaveTextContent('답변 등록하기');
  expect(screen.getByRole('button', { name: '답변' })).toBeInTheDocument();
});

test('상세 조회에 실패하면 오류 경계를 표시한다', async () => {
  const errors = jest.spyOn(console, 'error').mockImplementation(() => {});
  try {
    server.use(
      http.get('*/api/v1/feeds/:feedId', () =>
        HttpResponse.json(
          { status: 'error', code: 'POST_NOT_FOUND', message: '피드를 찾을 수 없습니다.' },
          { status: 404 },
        ),
      ),
    );
    show(999);
    expect(await screen.findByRole('alert')).toHaveTextContent('피드를 찾을 수 없습니다.');
  } finally {
    errors.mockRestore();
  }
});
