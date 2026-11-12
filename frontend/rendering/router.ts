import { routeTree } from '@/routeTree.gen';
import { dehydrate, hydrate as hydrateQueryClient, type QueryClient } from '@tanstack/react-query';
import { createRouter } from '@tanstack/react-router';
import { PATH_PARAMS_ALLOWED_CHARACTERS } from '@/constants/router';
import { RouteError } from '@/components/RouteError';

// 렌더 목적의 라우터에서는 단 하나의 진실 원천이 되는 라우터
export function createAppRouter(queryClient: QueryClient) {
  return createRouter({
    routeTree,
    scrollRestoration: true,
    defaultErrorComponent: RouteError,
    pathParamsAllowedCharacters: [...PATH_PARAMS_ALLOWED_CHARACTERS],
    dehydrate: () =>
      ({
        queryClientState: dehydrate(queryClient),
      }) as any,
    hydrate: (dehydrated) => {
      hydrateQueryClient(queryClient, dehydrated.queryClientState);
    },
  });
}

export type AppRouter = ReturnType<typeof createAppRouter>;

declare module '@tanstack/react-router' {
  interface Register {
    router: AppRouter;
  }
}
