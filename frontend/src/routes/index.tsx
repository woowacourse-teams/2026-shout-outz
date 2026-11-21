import { createFileRoute, type StaticDataRouteOption } from '@tanstack/react-router';

import {
  popularKeywordsQuery,
  popularTopicsQuery,
  recentMaterialsQuery,
  topicFieldsQuery,
} from '@/apis/explore';
import { jobPostingsQuery } from '@/apis/job-posting';
import { exploreProjectsQuery, projectTopicsQuery } from '@/apis/project-explore';

import type { ExploreTab } from '@/components/explore/ExploreHero';
import type { SearchResultKind } from '@/components/explore/ExploreSearchResult';
import { ExplorePage } from '@/pages/ExplorePage';

export interface ExploreSearch {
  /** 비어 있으면 키워드 탭 */
  tab?: 'job' | 'project';
  q?: string;
  /** 프로젝트 탭의 기술 주제 */
  topic?: string;
  /** 정렬. 비어 있으면 최신 등록순. 프로젝트 탭은 좋아요순, 채용공고 탭은 마감 임박순을 고를 수 있다. */
  sort?: 'POPULAR' | 'DEADLINE';
  /** 키워드 탭 검색 결과의 자료 종류. 비어 있으면 전체 */
  kind?: SearchResultKind;
}

const MATERIAL_KINDS: readonly SearchResultKind[] = ['PROJECT', 'BLOG', 'DOCUMENT', 'TALK'];
const isMaterialKind = (value: unknown): value is SearchResultKind =>
  MATERIAL_KINDS.includes(value as SearchResultKind);

const text = (value: unknown) => (typeof value === 'string' ? value.trim() : '');

// 서버 렌더 전에 받아 둘 쿼리. 페이지가 쓰는 것과 같은 쿼리 함수·인자여야 서버 렌더가 캐시를 찾는다.
// 서버 렌더 중에 useSuspenseQuery가 받은 데이터는 $_TSR에 담기지 않아 브라우저가 다시 받으므로, 미리 받아 셸과 $_TSR에 싣는다.
// 미리 받는 동안 첫 바이트가 늦어지므로 빠른 탭 첫 화면만 받는다. 검색어·기술 주제로 거른 화면은 검색 API가
// 느릴 수 있고 색인하지 않는 화면이라, 지금처럼 서버 렌더 중에 받아 스트리밍으로 이어 보낸다.
type SsrQueries = ReturnType<NonNullable<StaticDataRouteOption['ssrQueries']>>;

export function exploreSsrQueries({ tab, q, topic, sort }: ExploreSearch): SsrQueries {
  if (q || topic) return [];
  if (tab === 'job') return [jobPostingsQuery('', sort === 'DEADLINE' ? 'DEADLINE' : 'LATEST')];
  if (tab === 'project') {
    return [
      projectTopicsQuery(),
      exploreProjectsQuery({
        keyword: '',
        topicId: null,
        sort: sort === 'POPULAR' ? 'POPULAR' : 'LATEST',
      }),
    ];
  }
  return [
    recentMaterialsQuery('BLOG', 4),
    recentMaterialsQuery('TALK', 2),
    popularTopicsQuery(),
    topicFieldsQuery(),
    popularKeywordsQuery(),
  ];
}

// 탐색 화면이 홈이다. 검색어·탭이 주소마다 달라 빌드 때 미리 만들지 않고 요청마다 서버 렌더한다.
// 채용 공고 탭(?tab=job)과 프로젝트 탭도 같은 라우트라 함께 서버 렌더된다.
export const Route = createFileRoute('/')({
  staticData: {
    ssr: true,
    sitemap: true,
    ssrQueries: (_params, search) => exploreSsrQueries(search as ExploreSearch),
  },
  validateSearch: (search: Record<string, unknown>): ExploreSearch => {
    const tab = search.tab === 'job' || search.tab === 'project' ? search.tab : undefined;
    const q = text(search.q);
    const topic = tab === 'project' ? text(search.topic) : '';
    return {
      ...(tab && { tab }),
      ...(q && { q }),
      ...(topic && { topic }),
      ...(tab === 'project' && search.sort === 'POPULAR' && { sort: 'POPULAR' }),
      ...(tab === 'job' && search.sort === 'DEADLINE' && { sort: 'DEADLINE' }),
      // 자료 종류는 키워드 탭의 검색 결과에서만 쓴다.
      ...(!tab && q && isMaterialKind(search.kind) && { kind: search.kind }),
    };
  },
  component: ExploreRoute,
});

function ExploreRoute() {
  const search = Route.useSearch();
  const navigate = Route.useNavigate();
  const currentTab: ExploreTab = search.tab ?? 'keyword';
  const tabSearch = (tab: ExploreTab): ExploreSearch => (tab === 'keyword' ? {} : { tab });

  return (
    <ExplorePage
      tab={currentTab}
      // 탭마다 찾는 대상이 달라 탭을 바꾸면 검색어와 조건을 비운다.
      onTabChange={(tab) => void navigate({ search: tabSearch(tab) })}
      query={search.q ?? ''}
      // 프로젝트 탭의 기술 주제는 검색어처럼 다뤄, 검색하면 주제를 풀고 주제를 고르면 검색어를 비운다.
      // 검색어가 바뀌면 자료 종류도 전체로 되돌린다.
      onSearch={(q) =>
        void navigate({
          search: (prev) => ({ ...prev, q: q || undefined, topic: undefined, kind: undefined }),
        })
      }
      kind={search.kind ?? null}
      onKindChange={(kind) =>
        void navigate({
          search: (prev) => ({ ...prev, kind: kind ?? undefined }),
          resetScroll: false,
        })
      }
      topic={search.topic ?? null}
      // 주제와 정렬은 목록만 바꾸므로 스크롤 위치를 그대로 둔다.
      onTopicChange={(topic) =>
        void navigate({
          search: (prev) => ({ ...prev, q: undefined, topic: topic ?? undefined }),
          resetScroll: false,
        })
      }
      sort={search.sort === 'POPULAR' ? 'POPULAR' : 'LATEST'}
      jobSort={search.sort === 'DEADLINE' ? 'DEADLINE' : 'LATEST'}
      // 정렬은 목록만 바꾸므로 스크롤 위치를 그대로 둔다. 최신 등록순은 기본값이라 주소에서 뺀다.
      onSortChange={(sort) =>
        void navigate({
          search: (prev) => ({ ...prev, sort: sort === 'LATEST' ? undefined : sort }),
          resetScroll: false,
        })
      }
    />
  );
}
