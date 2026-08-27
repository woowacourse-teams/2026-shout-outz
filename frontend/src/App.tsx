import { QueryClientProvider, type QueryClient } from '@tanstack/react-query';
import { RouterProvider, type AnyRouter } from '@tanstack/react-router';

// ssg/client.tsx(CSR/하이드레이션)와 ssg/render.tsx(SSG)가 <Document><App .../></Document>로
// 동일한 트리를 그리기 위한 애플리케이션 루트입니다. router/queryClient는 각 entry가 이미
// 준비해서 넘겨주므로(render.tsx는 URL마다 새로, client.tsx는 부트스트랩 시 한 번),
// App 자신은 CSR/SSG 여부를 몰라도 됩니다.
export function App({
    router,
    queryClient,
}: {
    router: AnyRouter;
    queryClient: QueryClient;
}) {
    return (
        <QueryClientProvider client={queryClient}>
            <RouterProvider router={router} />
        </QueryClientProvider>
    );
}
