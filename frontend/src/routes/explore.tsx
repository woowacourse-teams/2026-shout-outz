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

// API가 생길 때까지 CSR로만 둔다(prerender·ssr을 지정하지 않는다).
export const Route = createFileRoute('/explore')({
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
