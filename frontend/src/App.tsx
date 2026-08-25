import { QueryClientProvider, HydrationBoundary, type QueryClient, type DehydratedState } from '@tanstack/react-query';
import { RouterProvider, type AnyRouter } from '@tanstack/react-router';

// entry-client.tsx(CSR/하이드레이션)와 entry-ssg.tsx(SSG)가 <Document><App .../></Document>로
// 동일한 트리를 그리기 위한 애플리케이션 루트입니다. router/queryClient는 각 entry가 이미
// 준비해서 넘겨주므로(entry-ssg.tsx는 URL마다 새로, entry-client.tsx는 부트스트랩 시 한 번),
// App 자신은 CSR/SSG 여부를 몰라도 됩니다.
//
// dehydratedState: entry-ssg.tsx가 렌더링 후 queryClient를 dehydrate해 HTML에 심어둔 값을,
// entry-client.tsx가 하이드레이트 시점에 다시 읽어 넘겨줍니다. 이게 없으면 클라이언트의
// queryClient는 빈 캐시로 시작해서 useSuspenseQuery가 즉시 서스펜드되어 Suspense fallback을
// 다시 그리는데, 서버가 이미 리졸브된 콘텐츠를 렌더링해뒀기 때문에 하이드레이션 불일치가 납니다.
export function App({
    router,
    queryClient,
    dehydratedState,
}: {
    router: AnyRouter;
    queryClient: QueryClient;
    dehydratedState?: DehydratedState;
}) {
    return (
        <QueryClientProvider client={queryClient}>
            <HydrationBoundary state={dehydratedState}>
                <RouterProvider router={router} />
            </HydrationBoundary>
        </QueryClientProvider>
    );
}
