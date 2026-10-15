import { routeTree } from '@/routeTree.gen';
import { createMemoryHistory, createRouter, type AnyRoute } from '@tanstack/react-router';
import { PATH_PARAMS_ALLOWED_CHARACTERS } from '@/constants/router';

function normalizePrerenderPath(pathname: string): string {
  return pathname === '/' ? pathname : pathname.replace(/\/+$/, '');
}

export interface PrerenderRouteFailure {
  route: string;
  error: unknown;
}

export interface PrerenderRouteCollection {
  routes: string[];
  failures: PrerenderRouteFailure[];
}

function createPathRouter() {
  return createRouter({
    routeTree,
    pathParamsAllowedCharacters: [...PATH_PARAMS_ALLOWED_CHARACTERS],
    history: createMemoryHistory({ initialEntries: ['/'] }),
  });
}

type PathRouter = ReturnType<typeof createPathRouter>;

function buildStaticPath(router: PathRouter, route: AnyRoute): string {
  // buildLocation으로 라우터의 경로 조립 규칙을 적용한 뒤, index route의 fullPath에
  // 남는 trailing slash를 기본 라우터 정책(never)에 맞게 제거합니다.
  const location = router.buildLocation({
    to: route.fullPath,
  } as unknown as Parameters<typeof router.buildLocation>[0]);
  return normalizePrerenderPath(location.pathname);
}

// 경로에 매치되는 라우트 id를 루트부터 순서대로 돌려줍니다. 페이지별 HTML에 넣을 라우트 청크를
// build-assets.json의 routeChunks에서 찾을 때 씁니다.
export function createRouteIdMatcher(): (pathname: string) => string[] {
  const router = createPathRouter();
  return (pathname) => router.matchRoutes(pathname).map((match) => match.routeId);
}

// 프리렌더하지 않는 정적 경로($param 없음)도 경로마다 CSR 셸을 따로 만들어, 그 경로의 라우트 청크를
// main과 함께 preload하게 합니다. 동적 경로는 미리 알 수 없어 공용 셸(dist/index.html)로 남습니다.
export function collectCsrShellRoutes(): string[] {
  const router = createPathRouter();
  const routes = new Set<string>();

  for (const route of Object.values(router.routesById) as AnyRoute[]) {
    if (route.isRoot || route.fullPath.includes('$') || route.options.staticData?.prerender) {
      continue;
    }
    routes.add(buildStaticPath(router, route));
  }

  return [...routes].sort();
}

// routeTree.gen.ts는 건드리지 않고, 라우터 인스턴스의 routesById를 순회해 각 라우트의
// staticData.prerender를 읽어 SSG 대상 경로 목록을 만듭니다. 동적 라우트는
// generateStaticParams가 돌려준 파라미터마다 경로를 펼칩니다.
export async function collectPrerenderRoutes(): Promise<PrerenderRouteCollection> {
  const router = createPathRouter();
  const routes = new Set<string>();
  const failures: PrerenderRouteFailure[] = [];

  for (const route of Object.values(router.routesById) as AnyRoute[]) {
    const staticData = route.options.staticData;
    if (!staticData?.prerender) continue;

    try {
      if (!route.fullPath.includes('$')) {
        routes.add(buildStaticPath(router, route));
        continue;
      }

      if (!staticData.generateStaticParams) {
        throw new Error(
          `[prerender] "${route.fullPath}"는 동적 라우트인데 generateStaticParams가 없습니다.`,
        );
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
    } catch (error) {
      failures.push({ route: route.fullPath, error });
    }
  }

  return { routes: [...routes].sort(), failures };
}
