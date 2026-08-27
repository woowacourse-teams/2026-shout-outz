import { createRoot, hydrateRoot } from 'react-dom/client';
import { QueryClient } from '@tanstack/react-query';
import { hydrate } from '@tanstack/react-router/ssr/client';
import { Document } from '../src/Document';
import { App } from '../src/App';
import { createAppRouter } from './router';
import '../src/index.css';

const queryClient = new QueryClient();
const router = createAppRouter(queryClient);

// document 자체가 React Root입니다. render.tsx가 그리는 트리와 정확히 같은
// <Document><App/></Document>를 그리고, 이 문서가 실제로 프리렌더됐는지에 따라 마운트 방식만
// 갈립니다(App은 이 분기를 모릅니다 - src/App.tsx 참고).
async function bootstrap() {
    const routerHydrationState = window.$_TSR;
    const tree = (
        <Document>
            <App router={router} queryClient={queryClient} />
        </Document>
    );

    if (!routerHydrationState) {
        // $_TSR이 없는 순수 CSR(dev 서버 등)에서는 하이드레이션 없이 새로 그립니다.
        createRoot(document).render(tree);
        return;
    }

    // Router 매치 상태와 React Query 캐시를 복원한 뒤 React 트리를 하이드레이트합니다.
    await hydrate(router).finally(() => routerHydrationState.h());
    hydrateRoot(document, tree);
}

void bootstrap();
