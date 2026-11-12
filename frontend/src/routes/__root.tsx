import { useSuspenseQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { feedQuery } from '@/apis/feed';
import { magazineQuery } from '@/apis/magazine';
import { projectDetailQueryOptions } from '@/api/project-detail';
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
import { toProjectSlug } from '@/utils/project';

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
  Component: (props: { id: string; pathname: string }) => ReactNode;
}> = [
  { pattern: /^\/magazines\/(\d+)\/?$/, Component: MagazineOpenGraph },
  { pattern: /^\/community\/(\d+)\/?$/, Component: FeedOpenGraph },
  // 경험·자료·소개 탭. 빌드할 때 SSG로 만드는 페이지라 결과 HTML의 <head>에 들어갑니다.
  {
    pattern: /^\/projects\/(?!new\/?$)([^/]+)(?:\/(?:materials|intro))?\/?$/,
    Component: ProjectOpenGraph,
  },
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
        <Component id={safeDecode(match[1] ?? '')} pathname={pathname.replace(/\/$/, '')} />
      </AsyncBoundary>
    );
  }
  return <OpenGraph />;
}

// 주소 글자가 깨져 있으면 디코딩이 throw하므로 원래 글자를 그대로 쓴다.
function safeDecode(segment: string) {
  try {
    return decodeURIComponent(segment);
  } catch {
    return segment;
  }
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

function MagazineOpenGraph({ id }: { id: string }) {
  const { data: magazine } = useSuspenseQuery(magazineQuery(Number(id)));
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
function FeedOpenGraph({ id }: { id: string }) {
  const { data: feed } = useSuspenseQuery(feedQuery(Number(id)));
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

// 화면(ProjectDetailContent)과 같은 쿼리라 SSG 빌드 때 받아 둔 데이터를 그대로 씁니다.
// 탭마다 내용이 달라 canonical은 지금 탭 주소로 둡니다.
function ProjectOpenGraph({ id, pathname }: { id: string; pathname: string }) {
  // 주소는 /projects/@slug라 화면처럼 @를 떼고 조회한다.
  const { data: project } = useSuspenseQuery(projectDetailQueryOptions(toProjectSlug(id)));
  // 승인 전 프로젝트는 화면도 보여 주지 않으므로 기본 OG를 둡니다.
  if (project.approvalStatus !== 'APPROVED') return <OpenGraph />;
  return (
    <OpenGraph
      title={project.title}
      description={project.tagline}
      image={project.imageUrl}
      pathname={pathname}
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
