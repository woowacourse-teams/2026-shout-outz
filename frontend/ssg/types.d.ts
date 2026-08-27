import '@tanstack/react-router';

declare module '@tanstack/react-router' {
    interface StaticDataRouteOption {
        /** true인 라우트만 SSG 프리렌더 대상에 포함됩니다. */
        prerender?: boolean;
        /** 동적 라우트($param 포함)에서 프리렌더할 파라미터 조합을 반환합니다. */
        generateStaticParams?: () => Promise<Array<Record<string, string>>>;
    }
}
