import { cleanup, render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
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
  server.use(...createFeedHandlers());
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

function show(postId: number) {
  client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  const router = createRouter({
    routeTree,
    history: createMemoryHistory({ initialEntries: [`/feeds/${postId}`] }),
  });
  render(
    <QueryClientProvider client={client}>
      <RouterContextProvider router={router}>
        <FeedDetailPage postId={postId} />
      </RouterContextProvider>
    </QueryClientProvider>,
  );
}

test('상세 URL에서 피드와 댓글을 각각 조회한다', async () => {
  show(1);
  expect(await screen.findByText(/Redis Pub\/Sub으로 WebSocket/)).toBeInTheDocument();
  await screen.findByRole('textbox', { name: '댓글 남기기' });
  expect(await screen.findByText('경험을 공유해 주셔서 감사합니다!')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: '좋아요' })).toBeDisabled();
});

test('상세 조회에 실패하면 오류 경계를 표시한다', async () => {
  const errors = jest.spyOn(console, 'error').mockImplementation(() => {});
  try {
    server.use(
      http.get('*/api/v1/posts/:postId', () =>
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
