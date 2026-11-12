import { useSuspenseQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { feedQuery } from '@/apis/feed';
import { magazineQuery } from '@/apis/magazine';
import { ClientOnly } from '@/components/ClientOnly';
import { FirstVisitGuide } from '@/components/FirstVisitGuide';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { exploreHead } from '@/components/explore/exploreHead';
import { OpenGraph } from '@/components/OpenGraph';
import { SITE_ORIGIN } from '@/constants/site';
import {
  createRootRoute,
  Outlet,
  useLocation,
  useMatch,
  useNavigate,
} from '@tanstack/react-router';
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
  const exploreSearch = useMatch({
    from: '/',
    shouldThrow: false,
    select: (match) => match.search,
  });

  if (exploreSearch) return <ExploreOpenGraph {...exploreSearch} />;

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

// 홈(탐색 화면)은 요청마다 서버 렌더하므로, 탭·검색어에 맞는 설명과 canonical이 서버 HTML의 <head>에 들어간다.
function ExploreOpenGraph({
  tab = 'keyword',
  q = '',
  topic = '',
}: {
  tab?: 'job' | 'project' | 'keyword';
  q?: string;
  topic?: string;
}) {
  const head = exploreHead({ tab, query: q, topic: topic || null });
  return (
    <>
      <OpenGraph
        title={head.title}
        description={head.description}
        pathname={head.canonicalPath ?? undefined}
        noindex={!head.canonicalPath}
      />
      {/* 검색 결과에 사이트 이름을 shout-outz로 보여 주도록 구글에 알린다. */}
      {head.canonicalPath === '/' && (
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{
            __html: JSON.stringify({
              '@context': 'https://schema.org',
              '@type': 'WebSite',
              name: 'shout-outz',
              alternateName: '샤라웃',
              url: `${SITE_ORIGIN}/`,
            }),
          }}
        />
      )}
    </>
  );
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
