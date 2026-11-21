import type { QueryClient } from '@tanstack/react-query';
import type { StaticDataRouteOption } from '@tanstack/react-router';

type SsrQuery = ReturnType<NonNullable<StaticDataRouteOption['ssrQueries']>>[number];

// 다음 페이지를 정하는 getNextPageParam은 무한 스크롤 쿼리에만 있습니다.
// initialPageParam은 undefined일 수 있어(exploreProjectsQuery) 판별에 쓰지 않습니다.
function isInfiniteQuery(
  query: SsrQuery,
): query is Extract<SsrQuery, { getNextPageParam: unknown }> {
  return 'getNextPageParam' in query;
}

/** 라우트의 staticData.ssrQueries를 서버 렌더 전에 캐시에 채웁니다. 하나라도 실패하면 reject합니다. */
export function ensureSsrQueries(queryClient: QueryClient, queries: SsrQuery[]) {
  return Promise.all(
    queries.map((query) =>
      isInfiniteQuery(query)
        ? queryClient.ensureInfiniteQueryData(query)
        : queryClient.ensureQueryData(query),
    ),
  );
}
