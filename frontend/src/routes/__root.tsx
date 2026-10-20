import { useSuspenseQuery } from '@tanstack/react-query';
import { magazineQuery } from '@/apis/magazine';
import { ClientOnly } from '@/components/ClientOnly';
import { FirstVisitGuide } from '@/components/FirstVisitGuide';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { OpenGraph } from '@/components/OpenGraph';
import { createRootRoute, Outlet, useLocation, useNavigate } from '@tanstack/react-router';
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools';

import { ResumeQuestionDraft } from '@/components/recommendations/ResumeQuestionDraft';
import { AnalyticsIdentifier } from '@/components/AnalyticsIdentifier';
import { BugReportButton } from '@/components/BugReportButton';
import { DevelopmentBanner } from '@/components/DevelopmentBanner';

// html/head/body는 Document(src/Document.tsx)의 책임입니다. 이 라우트는 라우팅 레이아웃
// 경계만 담당하고, 페이지별 레이아웃은 각 Page 컴포넌트에서 구성합니다.
const RootLayout = () => (
  <>
    <RouteOpenGraph />
    <FirstVisitGuideRoute />
    <AnalyticsIdentifier />
    <ResumeQuestionDraft />
    <DevelopmentBanner />
    <Outlet />
    <BugReportButton />
    <TanStackRouterDevtools />
  </>
);

export const Route = createRootRoute({ component: RootLayout });

function RouteOpenGraph() {
  const pathname = useLocation({ select: (location) => location.pathname });
  const match = /^\/magazines\/(\d+)\/?$/.exec(pathname);
  const magazineId = match ? Number(match[1]) : undefined;
  return magazineId ? (
    <AsyncBoundary key={magazineId} fallback={<OpenGraph />} errorFallback={() => <OpenGraph />}>
      <MagazineOpenGraph magazineId={magazineId} />
    </AsyncBoundary>
  ) : (
    <OpenGraph />
  );
}

function MagazineOpenGraph({ magazineId }: { magazineId: number }) {
  const { data: magazine } = useSuspenseQuery(magazineQuery(magazineId));
  return (
    <OpenGraph
      title={magazine.title}
      description={magazine.summary ?? undefined}
      image={magazine.thumbnailUrl}
      pathname={`/magazines/${magazine.id}`}
      article
    />
  );
}

function FirstVisitGuideRoute() {
  const pathname = useLocation({ select: (location) => location.pathname });
  const navigate = useNavigate();
  const mode = /^\/about\/?$/.test(pathname)
    ? 'visited'
    : /^\/(?:admin|signup)(?:\/|$)/.test(pathname) || /\/(?:new|edit)\/?$/.test(pathname)
      ? 'skip'
      : 'offer';
  return (
    <ClientOnly>
      <FirstVisitGuide
        key={pathname}
        mode={mode}
        onExplore={() => void navigate({ to: '/about' })}
      />
    </ClientOnly>
  );
}
