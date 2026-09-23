import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import { render } from '@testing-library/react';
import { setupServer } from 'msw/node';

import { handlers } from '@/api/mock/handlers';
import { ModalProvider } from '@/components/ModalProvider';
import { routeTree } from '@/routeTree.gen';

/**
 * 네트워크가 포함된 라우트 테스트의 공통 환경.
 *
 * 이 모듈을 import하는 테스트 파일은 최상단에 아래 도크블록이 있어야 한다. jest가 파일의
 * 첫 주석에서 환경을 읽기 때문에 import로는 대체할 수 없다.
 *
 * ```
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 * ```
 */
export const server = setupServer(...handlers);

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
  window.scrollTo = jest.fn();
});
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

/**
 * 실제 라우터와 QueryClient를 세워 화면을 그린다. App.tsx와 같은 순서로 감싸고,
 * QueryClient는 호출마다 새로 만들어 이전 테스트의 캐시가 넘어오지 않게 한다.
 *
 * 데이터가 도착할 때까지 기다리지 않는다. 렌더 직후에는 Suspense fallback이 떠 있고
 * fallback도 `<main>`을 그려서 여기서 기다리면 내용이 오기 전에 통과한다.
 * 실제 내용은 호출부가 `findBy*`로 기다려야 한다.
 */
export function renderRoute(entry: string) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  const router = createRouter({
    routeTree,
    history: createMemoryHistory({ initialEntries: [entry] }),
    scrollRestoration: false,
  });

  render(
    <QueryClientProvider client={queryClient}>
      <ModalProvider>
        <RouterProvider router={router} />
      </ModalProvider>
    </QueryClientProvider>,
  );

  return router;
}
