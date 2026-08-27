import { dehydrate, hydrate as hydrateQueryClient, type QueryClient } from '@tanstack/react-query';
import { createRouter } from '@tanstack/react-router';
import { routeTree } from '../src/routeTree.gen';

// 전달받은 QueryClient를 Router의 SSG 직렬화 생명주기에 연결합니다. 호출자는 브라우저에서는 한 번,
// 프리렌더에서는 페이지마다 QueryClient와 Router를 새로 만들어 상태가 섞이지 않게 합니다.
export function createAppRouter(queryClient: QueryClient) {
    return createRouter({
        routeTree,
        // React Query의 DehydratedState는 query key/data를 unknown으로 표현해 Router가
        // 직렬화 가능성을 정적으로 증명할 수 없습니다. 이 앱의 query key/data는 JSON 값만 사용하므로
        // Router에 넘기는 직렬화 경계에서만 타입을 단언합니다.
        dehydrate: () => ({
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
