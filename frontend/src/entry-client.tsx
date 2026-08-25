import { createRoot, hydrateRoot } from 'react-dom/client';
import { createRouter } from '@tanstack/react-router';
import { hydrate } from '@tanstack/router-core/ssr/client';
import { QueryClient, type DehydratedState } from '@tanstack/react-query';
import { routeTree } from './routeTree.gen';
import { Document } from './Document';
import { App } from './App';
import { isPrerendered } from './isPrerendered';
import './index.css';

const router = createRouter({ routeTree });
const queryClient = new QueryClient();

declare module '@tanstack/react-router' {
    interface Register {
        router: typeof router;
    }
}

// entry-ssg.tsx가 </body> 앞에 심어둔 <script id="__REACT_QUERY_STATE__">를 읽습니다.
// 이게 없으면 queryClient가 빈 캐시로 시작해 하이드레이션 시 서버와 다른 트리(Suspense
// fallback)를 그리게 됩니다.
function readDehydratedQueryState(): DehydratedState | undefined {
    const element = document.getElementById('__REACT_QUERY_STATE__');
    if (!element || !element.textContent) {
        return undefined;
    }
    return JSON.parse(element.textContent);
}

// document 자체가 React Root입니다. entry-ssg.tsx가 그리는 트리와 정확히 같은
// <Document><App/></Document>를 그리고, 이 문서가 실제로 프리렌더됐는지에 따라 마운트 방식만
// 갈립니다(App은 이 분기를 모릅니다 - src/App.tsx 참고).
async function bootstrap() {
    if (isPrerendered()) {
        const tree = (
            <Document>
                <App router={router} queryClient={queryClient} dehydratedState={readDehydratedQueryState()} />
            </Document>
        );

        // entry-ssg.tsx가 심어둔 $_TSR 하이드레이션 페이로드를 router에 채워 넣은 뒤에야
        // hydrateRoot를 호출합니다. RouterClient가 내부적으로 하는 일을 entry 레벨에서 직접 합니다.
        const tsr = (window as unknown as { $_TSR: { h(): void } }).$_TSR;
        await hydrate(router).finally(() => tsr.h());
        hydrateRoot(document, tree);
    } else {
        // $_TSR이 없는 순수 CSR(dev 서버 등)에서는 하이드레이션 없이 새로 그립니다.
        const tree = (
            <Document>
                <App router={router} queryClient={queryClient} />
            </Document>
        );
        createRoot(document).render(tree);
    }
}

bootstrap();
