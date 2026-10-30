import { useSuspenseQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { feedQuery } from '@/apis/feed';
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
import { toPlainText } from '@/utils/markdown';

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

// 상세 페이지별 OG. 경로의 첫 번째 캡처를 id로 넘기며, 어느 것에도 맞지 않으면 기본 OG를 그립니다.
const DETAIL_OPEN_GRAPHS: Array<{
  pattern: RegExp;
  Component: (props: { id: number }) => ReactNode;
}> = [
  { pattern: /^\/magazines\/(\d+)\/?$/, Component: MagazineOpenGraph },
  { pattern: /^\/community\/(\d+)\/?$/, Component: FeedOpenGraph },
];

function RouteOpenGraph() {
  const pathname = useLocation({ select: (location) => location.pathname });

  for (const { pattern, Component } of DETAIL_OPEN_GRAPHS) {
    const match = pattern.exec(pathname);
    if (!match) continue;
    return (
      <AsyncBoundary key={pathname} fallback={<OpenGraph />} errorFallback={() => <OpenGraph />}>
        <Component id={Number(match[1])} />
      </AsyncBoundary>
    );
  }
  return <OpenGraph />;
}

function MagazineOpenGraph({ id: magazineId }: { id: number }) {
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

// SSR 서버가 staticData.ssrQueries로 글을 먼저 받아 두므로, 서버 렌더에서 Suspense 없이 셸 <head>에 들어갑니다.
function FeedOpenGraph({ id: feedId }: { id: number }) {
  const { data: feed } = useSuspenseQuery(feedQuery(feedId));
  const [firstMedia] = [...feed.media].sort((a, b) => a.displayOrder - b.displayOrder);
  return (
    <OpenGraph
      title={feed.title}
      description={toPlainText(feed.content)}
      image={firstMedia?.url}
      pathname={`/community/${feed.feedId}`}
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
