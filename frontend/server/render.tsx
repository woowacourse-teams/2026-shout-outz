/// <reference types="node" />
import { Readable } from 'node:stream';
import type { ReadableStream as NodeReadableStream } from 'node:stream/web';
import { QueryClient } from '@tanstack/react-query';
import type { StaticDataRouteOption } from '@tanstack/react-router';
import { createRequestHandler, renderRouterToStream } from '@tanstack/react-router/ssr/server';
import { preinit, preload } from 'react-dom';
import type { BuildAssets, PreloadFiles } from '../ssg/assets';
import { createAppRouter } from '../ssg/router';
import { toQueryStateScript } from '../ssg/query-state';
import { insertBeforeBodyEnd } from './body-end';
import { App } from '@/App';
import { Document } from '@/Document';

type ServerAssetsProps = {
  buildAssets: BuildAssets;
  preloadFiles: PreloadFiles;
};

type StreamingPageOptions = ServerAssetsProps & {
  /** 매치된 라우트의 staticData.ssrQueries가 돌려준 쿼리 */
  queries: ReturnType<NonNullable<StaticDataRouteOption['ssrQueries']>>;
};

export type StreamingPage = {
  status: number;
  headers: Headers;
  /** redirect 응답이면 본문이 없습니다. */
  body: Readable | null;
};

// 스트리밍에서는 <head>가 첫 조각으로 먼저 나가서, 완성된 HTML 문자열에 태그를 끼워 넣는 injectBuildAssets를 쓸 수 없습니다.
// 대신 서버 렌더 중에 React에 리소스를 알려 주면 React가 셸의 <head>에 <link>를 넣습니다.
// 아무것도 그리지 않으므로 브라우저 트리에 이 컴포넌트가 없어도 하이드레이션 결과는 같습니다.
function ServerAssets({ buildAssets, preloadFiles }: ServerAssetsProps) {
  for (const file of buildAssets.styles ?? []) {
    preinit(file, { as: 'style' });
  }
  for (const file of preloadFiles.critical ?? []) {
    preload(file, { as: 'script' });
  }
  return null;
}

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
  // 여기까지 받은 쿼리는 $_TSR에 담기므로 스트림 끝 스크립트에 다시 싣지 않습니다.
  const inRouterState = new Set(
    queryClient
      .getQueryCache()
      .getAll()
      .map((query) => query.queryHash),
  );

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
        <Document>
          <ServerAssets buildAssets={buildAssets} preloadFiles={preloadFiles} />
          <App router={router} queryClient={queryClient} />
        </Document>
      ),
    }),
  );

  return {
    status: response.status,
    headers: response.headers,
    body: response.body
      ? withScripts(
          response.body,
          () => toQueryStateScript(queryClient, inRouterState) + toScriptTags(buildAssets.scripts),
        )
      : null,
  };
}

const toScriptTags = (scripts: string[]) =>
  scripts.map((file) => `<script type="module" src="${file}"></script>`).join('');

function withScripts(body: ReadableStream<Uint8Array>, html: () => string): Readable {
  const source = Readable.fromWeb(body as NodeReadableStream<Uint8Array>);
  const output = insertBeforeBodyEnd(html);
  // pipe는 앞 스트림의 에러를 넘기지 않아, 응답을 보내는 쪽이 output 하나만 보고 끊김을 알 수 있게 넘겨 줍니다.
  source.on('error', (error) => output.destroy(error));
  return source.pipe(output);
}
