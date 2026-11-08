import { createRoot, hydrateRoot } from 'react-dom/client';
import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query';
import { hydrate } from '@tanstack/react-router/ssr/client';
import '@/styles/index.css';
import { Document } from '@/Document';
import { getApiOrigin } from '@/utils/auth';
import { App } from '@/App';
import { createAppRouter } from '../rendering/router';
import { analytics } from '@/utils/analytics';
import { connectRouterPageViews } from '@/utils/analytics/connect';
import * as Sentry from '@sentry/react';

const queryClient = new QueryClient({
  queryCache: new QueryCache({ onError: (error) => Sentry.captureException(error) }),
  mutationCache: new MutationCache({ onError: (error) => Sentry.captureException(error) }),
});
const router = createAppRouter(queryClient);
const apiOrigin = getApiOrigin();

Sentry.init({
  dsn: process.env.NODE_ENV === 'production' ? process.env.SENTRY_DSN || undefined : undefined,
  environment: process.env.SENTRY_ENVIRONMENT,
  release: process.env.SENTRY_RELEASE || undefined,
  integrations: [Sentry.browserTracingIntegration()],
  // Tracing
  tracesSampleRate: 1.0, //  Capture 100% of the transactions
  ...(apiOrigin ? { tracePropagationTargets: [apiOrigin] } : {}),
});

const errorHandlers = {
  onUncaughtError: Sentry.reactErrorHandler(),
  onCaughtError: Sentry.reactErrorHandler(),
  onRecoverableError: Sentry.reactErrorHandler(),
};

async function bootstrap() {
  // API_ORIGIN ? 실제 서버 : MSW
  if (process.env.NODE_ENV === 'development' && !getApiOrigin()) {
    const { worker } = await import('@/mocks/browser');
    await worker.start({ onUnhandledRequest: 'bypass' });
  }
  connectRouterPageViews(router, analytics); // 라우터보다 먼저 모니터링 도구 연결

  const tree = (
    <Document>
      <App router={router} queryClient={queryClient} />
    </Document>
  );

  const routerHydrationState = window.$_TSR;

  // $_TSR이 없는 경우엔 createRoot
  if (!routerHydrationState) {
    createRoot(document, errorHandlers).render(tree);
    return;
  }

  await hydrate(router).finally(() => routerHydrationState.h()); // 청크 실행 / $_TSR queryClient로 옮기기 / 브라우저 라우터 매치 등등 .. 선행 작업
  hydrateRoot(document, tree, errorHandlers); // 렌더
}

void bootstrap();
