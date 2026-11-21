import { routeTree } from '@/routeTree.gen';
import {
  dehydrate,
  hydrate as hydrateQueryClient,
  type DehydratedState,
  type QueryClient,
} from '@tanstack/react-query';
import { createRouter } from '@tanstack/react-router';
import { PATH_PARAMS_ALLOWED_CHARACTERS } from '@/constants/router';
import { RouteError } from '@/components/RouteError';

const SERVER_DATA_STALE_TIME = 60_000;

// 전달받은 QueryClient를 Router의 SSG 직렬화 생명주기에 연결합니다. 호출자는 브라우저에서는 한 번,
// 프리렌더에서는 페이지마다 QueryClient와 Router를 새로 만들어 상태가 섞이지 않게 합니다.
export function createAppRouter(queryClient: QueryClient) {
  return createRouter({
    routeTree,
    scrollRestoration: true,
    defaultErrorComponent: RouteError,
    pathParamsAllowedCharacters: [...PATH_PARAMS_ALLOWED_CHARACTERS],
    // React Query의 DehydratedState는 query key/data를 unknown으로 표현해 Router가
    // 직렬화 가능성을 정적으로 증명할 수 없습니다. 이 앱의 query key/data는 JSON 값만 사용하므로
    // Router에 넘기는 직렬화 경계에서만 타입을 단언합니다.
    dehydrate: () =>
      ({
        queryClientState: dehydrate(queryClient),
      }) as any,
    hydrate: (dehydrated) => {
      const state: DehydratedState = dehydrated.queryClientState;
      hydrateQueryClient(queryClient, state);
      // useSuspenseQuery는 staleTime이 최소 1초라, 서버가 받은 데이터도 하이드레이션이 1초보다 늦으면 오래된 것으로 보고
      // 마운트하자마자 다시 받습니다. 서버에서 넘겨받은 쿼리만 잠시 새 데이터로 봅니다.
      // 오래됐는지는 서버가 받은 시각(dataUpdatedAt)으로 따지므로, 빌드 때 받은 SSG 데이터는 그대로 다시 받습니다.
      for (const { queryKey } of state.queries) {
        queryClient.setQueryDefaults(queryKey, { staleTime: SERVER_DATA_STALE_TIME });
      }
    },
  });
}

export type AppRouter = ReturnType<typeof createAppRouter>;

declare module '@tanstack/react-router' {
  interface Register {
    router: AppRouter;
  }
}
