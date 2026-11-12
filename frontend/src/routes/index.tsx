import { createFileRoute } from '@tanstack/react-router';

import type { ExploreTab } from '@/components/explore/ExploreHero';
import { ExplorePage } from '@/pages/ExplorePage';

export interface ExploreSearch {
  /** 비어 있으면 키워드 탭 */
  tab?: 'job' | 'project';
  q?: string;
  /** 프로젝트 탭의 기술 주제 */
  topic?: string;
  /** 프로젝트 탭 정렬. 비어 있으면 최신 등록순 */
  sort?: 'POPULAR';
}

const text = (value: unknown) => (typeof value === 'string' ? value.trim() : '');

// 탐색 화면이 홈이다. 검색어·탭이 주소마다 달라 빌드 때 미리 만들지 않고 요청마다 서버 렌더한다.
// 채용 공고 탭(?tab=job)과 프로젝트 탭도 같은 라우트라 함께 서버 렌더된다.
// 셸에 미리 채울 데이터는 없다. 목록은 페이지의 useSuspenseQuery가 서버 렌더 중에 받아 스트리밍으로 이어 보낸다.
export const Route = createFileRoute('/')({
  staticData: {
    ssr: true,
    sitemap: true,
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
      onSearch={(q) =>
        void navigate({ search: (prev) => ({ ...prev, q: q || undefined, topic: undefined }) })
      }
      topic={search.topic ?? null}
      // 주제와 정렬은 목록만 바꾸므로 스크롤 위치를 그대로 둔다.
      onTopicChange={(topic) =>
        void navigate({
          search: (prev) => ({ ...prev, q: undefined, topic: topic ?? undefined }),
          resetScroll: false,
        })
      }
      sort={search.sort ?? 'LATEST'}
      onSortChange={(sort) =>
        void navigate({
          search: (prev) => ({ ...prev, sort: sort === 'POPULAR' ? 'POPULAR' : undefined }),
          resetScroll: false,
        })
      }
    />
  );
}
