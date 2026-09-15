/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { render, screen, within } from '@testing-library/react';
import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { handlers } from '@/api/mock/handlers';
import { routeTree } from '@/routeTree.gen';

const server = setupServer(...handlers);

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
  window.scrollTo = jest.fn();
});
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

function renderPage(path = '/projects') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  const router = createRouter({
    routeTree,
    history: createMemoryHistory({ initialEntries: [path] }),
    scrollRestoration: false,
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
}

test('목록 API 응답을 카드로 표시한다', async () => {
  renderPage();
  expect(await screen.findByRole('heading', { name: 'Dropit' })).toBeInTheDocument();
  expect(screen.getAllByRole('listitem')).toHaveLength(3);
  expect(screen.getByRole('heading', { name: 'Shout-outz' })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Code Review Bot' })).toBeInTheDocument();
});

test('빈 목록을 안내한다', async () => {
  server.use(
    http.get('/api/v1/projects', () => HttpResponse.json({ status: 'success', data: [] })),
  );
  renderPage();
  expect(await screen.findByText('등록된 프로젝트가 없습니다.')).toBeInTheDocument();
});

test('조회 실패 후 다시 시도하면 목록을 표시한다', async () => {
  server.use(
    http.get('/api/v1/projects', () => new HttpResponse(null, { status: 500 }), { once: true }),
  );
  const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
  try {
    renderPage();
    const retryButton = await screen.findByRole('button', { name: '다시 시도' });
    expect(screen.getByRole('banner')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: '프로젝트' })).toBeInTheDocument();
    expect(screen.getByRole('contentinfo')).toBeInTheDocument();

    await userEvent.click(retryButton);
    expect(await screen.findByRole('heading', { name: 'Dropit' })).toBeInTheDocument();
  } finally {
    consoleError.mockRestore();
  }
});

test.each(['/projects', '/projects/'])(
  '%s 직접 진입 시 서비스 레이아웃과 목록을 표시한다',
  async (path) => {
    renderPage(path);

    expect(await screen.findByRole('heading', { name: 'Dropit' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: '프로젝트' })).toBeInTheDocument();
    const header = screen.getByRole('banner');
    expect(within(header).getByRole('link', { name: '프로젝트' })).toHaveAttribute(
      'aria-current',
      'page',
    );
    expect(screen.getByRole('navigation', { name: '서비스 바로가기' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Home' })).not.toBeInTheDocument();
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
  },
);
