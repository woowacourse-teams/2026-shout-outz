import { createMemoryHistory, createRouter, type AnyRoute } from '@tanstack/react-router';
import { routeTree } from '../src/routeTree.gen';

function normalizePrerenderPath(pathname: string): string {
    return pathname === '/' ? pathname : pathname.replace(/\/+$/, '');
}

// routeTree.gen.ts는 건드리지 않고, 라우터 인스턴스의 routesById를 순회해 각 라우트의
// staticData.prerender를 읽어 SSG 대상 경로 목록을 만듭니다. 동적 라우트는
// generateStaticParams가 돌려준 파라미터마다 경로를 펼칩니다.
export async function collectPrerenderRoutes(): Promise<string[]> {
    const router = createRouter({
        routeTree,
        history: createMemoryHistory({ initialEntries: ['/'] }),
    });
    const routes = new Set<string>();

    for (const route of Object.values(router.routesById) as AnyRoute[]) {
        const staticData = route.options.staticData;
        if (!staticData?.prerender) continue;

        if (!route.fullPath.includes('$')) {
            // buildLocation으로 라우터의 경로 조립 규칙을 적용한 뒤, index route의 fullPath에
            // 남는 trailing slash를 기본 라우터 정책(never)에 맞게 제거합니다.
            const location = router.buildLocation({
                to: route.fullPath,
            } as unknown as Parameters<typeof router.buildLocation>[0]);
            routes.add(normalizePrerenderPath(location.pathname));
            continue;
        }

        if (!staticData.generateStaticParams) {
            throw new Error(`[prerender] "${route.fullPath}"는 동적 라우트인데 generateStaticParams가 없습니다.`);
        }

        const paramSets = await staticData.generateStaticParams();
        for (const params of paramSets) {
            // 인코딩, 커스텀 params.stringify, splat/optional 세그먼트까지 라우터의
            // 경로 조립 로직(interpolatePath)에 위임합니다. fullPath가 런타임에 동적으로
            // 결정되는 문자열이라 등록된 라우트 리터럴 유니온으로는 좁혀지지 않아 캐스팅합니다.
            const location = router.buildLocation({
                to: route.fullPath,
                params,
            } as unknown as Parameters<typeof router.buildLocation>[0]);
            routes.add(normalizePrerenderPath(location.pathname));
        }
    }

    return [...routes].sort();
}
