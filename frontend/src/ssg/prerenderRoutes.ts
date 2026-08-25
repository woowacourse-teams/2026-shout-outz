import type { AnyRoute, AnyRouter } from '@tanstack/react-router';

declare module '@tanstack/react-router' {
    interface StaticDataRouteOption {
        /** true인 라우트만 SSG 프리렌더 대상에 포함됩니다. */
        prerender?: boolean;
        /** 동적 라우트($param 포함)에서 프리렌더할 파라미터 조합을 반환합니다. */
        generateStaticParams?: () => Promise<Array<Record<string, string>>>;
    }
}

// routeTree.gen.ts는 건드리지 않고, 라우터 인스턴스의 routesById를 순회해 각 라우트의
// staticData.prerender를 읽어 SSG 대상 경로 목록을 만듭니다. 동적 라우트는
// generateStaticParams가 돌려준 파라미터마다 경로를 펼칩니다.
export async function collectPrerenderRoutes(router: AnyRouter): Promise<string[]> {
    const routes: string[] = [];

    for (const route of Object.values(router.routesById) as AnyRoute[]) {
        const staticData = route.options.staticData;
        if (!staticData?.prerender) continue;

        if (!route.fullPath.includes('$')) {
            // fullPath('/project-detail/' 같은)를 그대로 쓰지 않고 buildLocation을 거칩니다.
            // trailingSlash 정규화 등 라우터가 매칭에 기대하는 경로 형태로 맞춰줍니다.
            const location = router.buildLocation({
                to: route.fullPath,
            } as unknown as Parameters<AnyRouter['buildLocation']>[0]);
            routes.push(location.pathname);
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
            } as unknown as Parameters<AnyRouter['buildLocation']>[0]);
            routes.push(location.pathname);
        }
    }

    return routes;
}
