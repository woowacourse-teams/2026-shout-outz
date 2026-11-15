/// <reference types="node" />
import { Readable, Transform } from 'node:stream';
import { StringDecoder } from 'node:string_decoder';
import type { ReadableStream as NodeReadableStream } from 'node:stream/web';
import { QueryClient } from '@tanstack/react-query';
import type { StaticDataRouteOption } from '@tanstack/react-router';
import { createRequestHandler, renderRouterToStream } from '@tanstack/react-router/ssr/server';
import { preinit, preload } from 'react-dom';
import type { BuildAssets, PreloadFiles } from '../ssg/assets';
import { createAppRouter } from '../ssg/router';
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

const BODY_END = '</body>';

// main script를 </body> 직전에 끼워 넣습니다. React의 bootstrapModules는 async를 붙여, 스트림 끝에 오는
// $_TSR보다 main이 먼저 실행될 수 있습니다. async가 없는 module script는 문서를 다 읽은 뒤 실행됩니다.
// </body>가 두 조각에 걸쳐 올 수 있어 마지막 몇 글자는 다음 조각과 이어 붙여 찾습니다.
function insertBeforeBodyEnd(html: string): Transform {
  const decoder = new StringDecoder('utf-8');
  let pending = '';
  let inserted = false;

  return new Transform({
    transform(chunk: Buffer, _encoding, callback) {
      const text = pending + decoder.write(chunk);
      if (inserted) return callback(null, text);

      const index = text.indexOf(BODY_END);
      if (index !== -1) {
        inserted = true;
        pending = '';
        return callback(null, text.slice(0, index) + html + text.slice(index));
      }

      const keep = BODY_END.length - 1;
      pending = text.slice(-keep);
      callback(null, text.slice(0, -keep));
    },
    flush(callback) {
      callback(null, pending + decoder.end());
    },
  });
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
    body: response.body ? withScripts(response.body, buildAssets.scripts) : null,
  };
}

function withScripts(body: ReadableStream<Uint8Array>, scripts: string[]): Readable {
  const source = Readable.fromWeb(body as NodeReadableStream<Uint8Array>);
  const output = insertBeforeBodyEnd(
    scripts.map((file) => `<script type="module" src="${file}"></script>`).join(''),
  );
  // pipe는 앞 스트림의 에러를 넘기지 않아, 응답을 보내는 쪽이 output 하나만 보고 끊김을 알 수 있게 넘겨 줍니다.
  source.on('error', (error) => output.destroy(error));
  return source.pipe(output);
}
