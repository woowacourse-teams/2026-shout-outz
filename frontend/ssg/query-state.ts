import {
  dehydrate,
  hydrate as hydrateQueryClient,
  type DehydratedState,
  type Query,
  type QueryClient,
} from '@tanstack/react-query';

declare global {
  interface Window {
    /** 서버 렌더 중에 받은 쿼리. SSR 서버가 스트림 끝(</body> 직전)에 넣습니다. */
    __SSR_QUERY_STATE__?: DehydratedState;
  }
}

// useSuspenseQuery는 staleTime이 최소 1초라, 서버가 받은 데이터도 하이드레이션이 1초보다 늦으면 오래된 것으로 보고
// 마운트하자마자 다시 받습니다. 서버에서 넘겨받은 쿼리는 이 시간 동안 새 데이터로 봅니다.
const SERVER_DATA_STALE_TIME = 60_000;

/**
 * 서버 렌더가 끝난 시점의 쿼리 캐시를 <script>로 만듭니다.
 * TanStack Router는 서버 렌더를 시작하기 전에 $_TSR에 담을 캐시를 dehydrate해서, 렌더 중 useSuspenseQuery가
 * 받은 데이터는 $_TSR에 없습니다. React는 Suspense가 모두 풀린 뒤에 </body>를 보내므로, 그 직전에 만들면
 * 셸을 늦추지 않고 렌더 중 받은 데이터를 모두 실을 수 있습니다.
 * `skip`에 든 쿼리(이미 $_TSR에 담긴 것)는 다시 싣지 않습니다.
 */
export function toQueryStateScript(queryClient: QueryClient, skip: ReadonlySet<string>): string {
  const state = dehydrate(queryClient, {
    shouldDehydrateQuery: (query: Query) =>
      query.state.status === 'success' && !skip.has(query.queryHash),
  });
  if (state.queries.length === 0) return '';
  // </script>나 줄 구분 문자가 데이터에 있어도 스크립트가 끊기지 않게 이스케이프합니다.
  const json = JSON.stringify(state)
    .replace(/</g, '\\u003c')
    .replace(/\u2028/g, '\\u2028')
    .replace(/\u2029/g, '\\u2029');
  return `<script>self.__SSR_QUERY_STATE__=${json};document.currentScript.remove()</script>`;
}

/** 서버에서 넘겨받은 쿼리를 캐시에 넣고, 하이드레이션 직후 다시 받지 않게 합니다. */
export function hydrateServerQueries(queryClient: QueryClient, state: DehydratedState | undefined) {
  if (!state) return;
  hydrateQueryClient(queryClient, state);
  // 오래됐는지는 서버가 받은 시각(dataUpdatedAt)으로 따지므로, 빌드 때 받은 SSG 데이터는 그대로 다시 받습니다.
  for (const { queryKey } of state.queries) {
    queryClient.setQueryDefaults(queryKey, { staleTime: SERVER_DATA_STALE_TIME });
  }
}
