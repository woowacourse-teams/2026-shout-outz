import { routeTree } from '@/routeTree.gen';
import { createMemoryHistory, createRouter } from '@tanstack/react-router';
import { PATH_PARAMS_ALLOWED_CHARACTERS } from '@/constants/router';

// SSG, SSR 파이프라인에서 사용할 경로 매칭용(matchRoutes, buildLocation, routesById) 라우터
export function createPathRouter() {
  return createRouter({
    routeTree,
    pathParamsAllowedCharacters: [...PATH_PARAMS_ALLOWED_CHARACTERS],
    history: createMemoryHistory({ initialEntries: ['/'] }),
  });
}

export type PathRouter = ReturnType<typeof createPathRouter>;
