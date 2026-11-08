/// <reference types="node" />
import { Readable } from 'node:stream';
import type { ReadableStream as NodeReadableStream } from 'node:stream/web';
import { QueryClient } from '@tanstack/react-query';
import type { StaticDataRouteOption } from '@tanstack/react-router';
import { createRequestHandler, renderRouterToStream } from '@tanstack/react-router/ssr/server';
import { BuildAssetLinks, withBuildScripts, type PageAssets } from '../rendering/BuildAssetTags';
import { createAppRouter } from '../rendering/router';
import { App } from '@/App';
import { Document } from '@/Document';

type StreamingPageOptions = PageAssets & {
  /** 매치된 라우트의 staticData.ssrQueries가 돌려준 쿼리 */
  queries: ReturnType<NonNullable<StaticDataRouteOption['ssrQueries']>>;
};

export type StreamingPage = {
  status: number;
  headers: Headers;
  /** redirect 응답이면 본문이 없습니다. */
  body: Readable | null;
};

// 요청마다 TanStack Router의 요청 핸들러로 페이지를 스트리밍 서버 렌더합니다.
// 핸들러가 라우트 매치(router.load), $_TSR 직렬화, 크롤러 판별(크롤러에게는 Suspense가 다 풀린 뒤 전송),
// 요청이 끊겼을 때의 중단을 맡고, 여기서는 우리 빌드의 CSS·script를 넣는 일만 합니다.
// 셸을 만들기 전에 실패하면 reject하므로, 호출자는 아직 응답을 시작하지 않은 상태에서 대체 HTML을 보낼 수 있습니다.
export async function renderStreamingPage(
  request: Request,
  { buildAssets, preloadFiles, queries }: StreamingPageOptions,
): Promise<StreamingPage> {
  // 요청마다 새로 만들어 다른 사용자의 쿼리 캐시가 섞이지 않게 합니다.
  const queryClient = new QueryClient();

  // 핸들러는 콜백( 서버 렌더)을 부르기 전에 $_TSR에 담을 상태를 dehydrate하므로, 그보다 먼저 캐시를 채웁니다.
  // 페이지의 useSuspenseQuery는 서버 렌더 중에 이 캐시를 그대로 씁니다.
  // 요청이 실패하면(없는 글의 404 등) reject해 호출자가 셸 대신 다른 응답을 보내게 합니다.
  await Promise.all(queries.map((query) => queryClient.ensureQueryData(query)));

  const handler = createRequestHandler({
    request,
    createRouter: () => createAppRouter(queryClient),
  });

  const response = await handler(({ request, router, responseHeaders }) =>
    renderRouterToStream({
      request,
      router,
      responseHeaders,
      children: (
        <>
          <BuildAssetLinks buildAssets={buildAssets} preloadFiles={preloadFiles} />
          <Document>
            <App router={router} queryClient={queryClient} />
          </Document>
        </>
      ),
    }),
  );

  return {
    status: response.status,
    headers: response.headers,
    body: response.body
      ? withBuildScripts(
          Readable.fromWeb(response.body as NodeReadableStream<Uint8Array>),
          buildAssets.scripts,
        )
      : null,
  };
}
