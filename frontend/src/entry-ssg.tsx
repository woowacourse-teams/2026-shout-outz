/// <reference types="node" />
import type { Readable } from 'node:stream';
import type { ReactNode } from 'react';
import { prerenderToNodeStream } from 'react-dom/static';
import { QueryClient, dehydrate } from '@tanstack/react-query';
import { createRouter, createMemoryHistory, type AnyRouter } from '@tanstack/react-router';
import { attachRouterServerSsrUtils, transformPipeableStreamWithRouter } from '@tanstack/react-router/ssr/server';
import { Document } from './Document';
import { App } from './App';
import { routeTree } from './routeTree.gen';

// entry-client.tsx의 서버 짝입니다. "URL 하나를 어떻게 HTML로 렌더링하는지"만 알고, 그 결과로
// 무엇을 할지(파일로 쓸지, 몇 개 라우트를 돌지, build-assets를 어디서 읽을지)는 모릅니다
// — 그건 src/ssg/generateStaticSite.ts의 몫입니다.
async function renderToHtml(node: ReactNode): Promise<string> {
    const { prelude } = await prerenderToNodeStream(node, {
        onError: (error: unknown) => console.error('[render error]', error),
    });

    const chunks: Buffer[] = [];
    for await (const chunk of prelude) {
        chunks.push(chunk as Buffer);
    }
    return Buffer.concat(chunks).toString('utf-8');
}

function insertBeforeBodyClose(html: string, insertion: string): string {
    const index = html.indexOf('</body>');
    if (index === -1) {
        throw new Error('renderPage: </body>를 찾지 못했습니다.');
    }
    return `${html.slice(0, index)}${insertion}${html.slice(index)}`;
}

// App(라우터) 없이 Document만 렌더링한 CSR fallback 셸입니다. SSG로 커버되지 않는 경로에서
// 브라우저가 client entry를 내려받아 처음부터 클라이언트 렌더링을 시작하는 진입점입니다.
export async function renderCsrShell(): Promise<string> {
    return renderToHtml(<Document />);
}

// prerenderToNodeStream은 API 호출을 포함한 모든 Suspense 콘텐츠가 끝날 때까지 기다린 뒤 HTML을 만듭니다.
// transformPipeableStreamWithRouter는 그 결과 스트림에서 </body> 직전을 찾아 TanStack Router의
// $_TSR 하이드레이션 데이터를 끼워 넣습니다(Document가 문서 전체를 그려야만 </body>를 찾을 수 있습니다).
export async function renderPage(url: string): Promise<string> {
    const router = createRouter({
        routeTree,
        history: createMemoryHistory({ initialEntries: [url] }),
    }) as AnyRouter;
    const queryClient = new QueryClient();

    // router.serverSsr을 세팅해야 transformPipeableStreamWithRouter가 $_TSR 부트스트랩을 주입할 수 있습니다.
    attachRouterServerSsrUtils({ router, manifest: undefined });

    // 브라우저에서는 마운트 시 자동으로 처리되지만, 서버에는 그 마운트 이펙트가 없어 직접 호출해야 합니다.
    await router.load();

    const { prelude } = await prerenderToNodeStream(
        <Document>
            <App router={router} queryClient={queryClient} />
        </Document>,
        { onError: (error: unknown) => console.error(`[render error] ${url}`, error) }
    );

    // createRequestHandler를 쓰지 않는 커스텀 SSG라, 매치 상태를 $_TSR로 직렬화하는 걸 직접 트리거해야 합니다.
    await router.serverSsr?.dehydrate();

    const withTsr = transformPipeableStreamWithRouter(router, prelude as unknown as Readable, {});

    const chunks: Buffer[] = [];
    for await (const chunk of withTsr) {
        chunks.push(chunk as Buffer);
    }
    let html = Buffer.concat(chunks).toString('utf-8');

    // TanStack Router의 $_TSR은 라우트 매치 상태만 담고, useSuspenseQuery로 받아온 React Query
    // 캐시는 별도로 dehydrate해서 심어야 합니다. 이게 없으면 클라이언트가 빈 캐시로 하이드레이트를
    // 시작해서 서버가 그린 리졸브된 콘텐츠와 다른(Suspense fallback) 트리를 그리게 됩니다.
    const dehydratedState = JSON.stringify(dehydrate(queryClient)).replace(/</g, '\\u003c');
    html = insertBeforeBodyClose(html, `<script id="__REACT_QUERY_STATE__" type="application/json">${dehydratedState}</script>`);

    return html;
}
