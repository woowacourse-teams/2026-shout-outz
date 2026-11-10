import { createFileRoute } from '@tanstack/react-router';

import type { ExploreTab } from '@/components/explore/ExploreHero';
import { ExplorePage } from '@/pages/ExplorePage';

interface ExploreSearch {
  /** 비어 있으면 키워드 탭 */
  tab?: 'job';
  q?: string;
}

// API가 생길 때까지 CSR로만 둔다(prerender·ssr을 지정하지 않는다).
export const Route = createFileRoute('/explore')({
  validateSearch: (search: Record<string, unknown>): ExploreSearch => {
    const q = typeof search.q === 'string' ? search.q.trim() : '';
    return {
      ...(search.tab === 'job' && { tab: 'job' }),
      ...(q && { q }),
    };
  },
  component: ExploreRoute,
});

function ExploreRoute() {
  const { tab, q } = Route.useSearch();
  const navigate = Route.useNavigate();
  const currentTab: ExploreTab = tab ?? 'keyword';
  const searchOf = (nextTab: ExploreTab, query?: string): ExploreSearch => ({
    ...(nextTab === 'job' && { tab: 'job' }),
    ...(query && { q: query }),
  });

  return (
    <ExplorePage
      tab={currentTab}
      // 탭마다 찾는 대상이 달라 탭을 바꾸면 검색어를 비운다.
      onTabChange={(nextTab) => void navigate({ search: searchOf(nextTab) })}
      query={q ?? ''}
      onSearch={(query) => void navigate({ search: searchOf(currentTab, query) })}
    />
  );
}
