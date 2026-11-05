import '@tanstack/react-router';
import type { EnsureQueryDataOptions } from '@tanstack/react-query';

declare module '@tanstack/react-router' {
  interface StaticDataRouteOption {
    /** true인 라우트만 SSG 프리렌더 대상에 포함됩니다. */
    prerender?: boolean;
    /** 동적 라우트($param 포함)에서 프리렌더할 파라미터 조합을 반환합니다. */
    generateStaticParams?: () => Promise<Array<Record<string, string>>>;
    /** true인 라우트는 요청마다 SSR 서버(server/index.ts)에서 렌더합니다. */
    ssr?: boolean;
    /**
     * SSR 서버가 서버 렌더 전에 채워 둘 쿼리를 반환합니다. 페이지가 useSuspenseQuery로 쓰는 것과
     * 같은 쿼리 함수(feedQuery 등)를 돌려줘야 서버 렌더가 캐시를 찾습니다.
     */
    // 쿼리마다 데이터 타입이 달라 한 배열에 담으려면 타입 인자를 열어 둬야 합니다. unknown으로 두면
    // staleTime 같은 콜백 옵션의 인자 타입이 맞지 않아 feedQuery(...)를 넣을 수 없습니다.
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ssrQueries?: (params: Record<string, string>) => EnsureQueryDataOptions<any, any, any, any>[];
  }
}
