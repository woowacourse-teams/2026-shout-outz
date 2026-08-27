/// <reference types="node" />
import type { Readable } from 'node:stream';
import type { ReactNode } from 'react';
import { QueryClient } from '@tanstack/react-query';
import { prerenderToNodeStream } from 'react-dom/static';
import { createMemoryHistory } from '@tanstack/react-router';
import { attachRouterServerSsrUtils, transformPipeableStreamWithRouter } from '@tanstack/react-router/ssr/server';
import { Document } from '../src/Document';
import { App } from '../src/App';
import { createAppRouter } from './router';

async function streamToString(stream: AsyncIterable<unknown>): Promise<string> {
    const chunks: Buffer[] = [];

    for await (const chunk of stream) {
        chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk as Uint8Array));
    }

    return Buffer.concat(chunks).toString('utf-8');
}

async function renderToStream(node: ReactNode, context: string): Promise<Readable> {
    const errors: unknown[] = [];
    const { prelude } = await prerenderToNodeStream(node, {
        onError: (error: unknown) => {
            errors.push(error);
        },
    });

    if (errors.length > 0) {
        throw new AggregateError(errors, `[render error] ${context}`);
    }

    return prelude as unknown as Readable;
}

// App(라우터) 없이 Document만 렌더링한 CSR fallback 셸입니다. SSG로 커버되지 않는 경로에서
// 브라우저가 client entry를 내려받아 처음부터 클라이언트 렌더링을 시작하는 진입점입니다.
export async function renderCsrShell(): Promise<string> {
    return streamToString(await renderToStream(<Document />, 'CSR shell'));
}

// prerenderToNodeStream은 API 호출을 포함한 모든 Suspense 콘텐츠가 끝날 때까지 기다린 뒤 HTML을 만듭니다.
// transformPipeableStreamWithRouter는 그 결과 스트림에서 </body> 직전을 찾아 TanStack Router의
// $_TSR 하이드레이션 데이터를 끼워 넣습니다(Document가 문서 전체를 그려야만 </body>를 찾을 수 있습니다).
export async function renderStaticPage(url: string): Promise<string> {
    const queryClient = new QueryClient();
    const router = createAppRouter(queryClient);

    // router.serverSsr을 세팅해야 transformPipeableStreamWithRouter가 $_TSR 부트스트랩을 주입할 수 있습니다.
    attachRouterServerSsrUtils({ router, manifest: undefined });

    try {
        router.update({
            history: createMemoryHistory({ initialEntries: [url] }),
        });

        // 브라우저에서는 마운트 시 자동으로 처리되지만, 서버에는 그 마운트 이펙트가 없어 직접 호출해야 합니다.
        await router.load();

        const stream = await renderToStream(
            <Document>
                <App router={router} queryClient={queryClient} />
            </Document>,
            url
        );

        // createRequestHandler를 쓰지 않는 커스텀 SSG라, 매치 상태를 $_TSR로 직렬화하는 걸 직접 트리거해야 합니다.
        // React Query 데이터는 위 렌더 과정에서 채워지므로 렌더가 끝난 다음 직렬화해야 합니다.
        await router.serverSsr?.dehydrate();

        return streamToString(transformPipeableStreamWithRouter(router, stream, {}));
    } catch (error) {
        router.serverSsr?.cleanup();
        throw error;
    }
}
