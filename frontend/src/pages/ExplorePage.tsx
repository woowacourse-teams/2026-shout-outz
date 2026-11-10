import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { ExploreHero, type ExploreTab } from '@/components/explore/ExploreHero';
import { ExploreHome } from '@/components/explore/ExploreHome';
import { ExploreSearchResult } from '@/components/explore/ExploreSearchResult';
import { Footer } from '@/components/Footer';
import { JobPostingList } from '@/components/job-postings/JobPostingList';

interface ExplorePageProps {
  tab: ExploreTab;
  onTabChange: (tab: ExploreTab) => void;
  /** 검색어. 비어 있으면 검색 전 화면을 보여 준다. */
  query: string;
  onSearch: (query: string) => void;
}

export function ExplorePage({ tab, onTabChange, query, onSearch }: ExplorePageProps) {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>{query ? `${query} 검색 결과 | shout-outz` : '탐색 | shout-outz'}</title>
      <AppGnb />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 pt-12 pb-18 break-keep md:pt-20 md:pb-28">
        <ExploreHero tab={tab} onTabChange={onTabChange} query={query} onSearch={onSearch} />
        {tab === 'job' ? (
          <div className="mt-10">
            <AsyncBoundary>
              <JobPostingList keyword={query} />
            </AsyncBoundary>
          </div>
        ) : (
          <div className="mx-auto max-w-4xl">
            {query ? (
              <div className="mt-10" aria-live="polite">
                <AsyncBoundary>
                  {/* 검색어가 바뀌면 종류 탭을 '전체'로 되돌린다. */}
                  <ExploreSearchResult key={query} query={query} />
                </AsyncBoundary>
              </div>
            ) : (
              <AsyncBoundary>
                <ExploreHome onSearch={onSearch} />
              </AsyncBoundary>
            )}
          </div>
        )}
      </main>
      <Footer />
    </div>
  );
}
