import { useQuery } from '@tanstack/react-query';

import { popularKeywordsQuery } from '@/apis/explore';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { ExploreHero, type ExploreTab } from '@/components/explore/ExploreHero';
import { ExploreHome } from '@/components/explore/ExploreHome';
import { ExploreSearchResult } from '@/components/explore/ExploreSearchResult';
import { Footer } from '@/components/Footer';
import { JobPostingList } from '@/components/job-postings/JobPostingList';
import { ProjectExplore } from '@/components/project-explore/ProjectExplore';
import type { ProjectSort } from '@/types/project';

interface ExplorePageProps {
  tab: ExploreTab;
  onTabChange: (tab: ExploreTab) => void;
  /** 검색어. 비어 있으면 검색 전 화면을 보여 준다. */
  query: string;
  onSearch: (query: string) => void;
  /** 프로젝트 탭의 기술 주제 */
  topic: string | null;
  onTopicChange: (topic: string | null) => void;
  sort: ProjectSort;
  onSortChange: (sort: ProjectSort) => void;
}

export function ExplorePage({
  tab,
  onTabChange,
  query,
  onSearch,
  topic,
  onTopicChange,
  sort,
  onSortChange,
}: ExplorePageProps) {
  const popularKeywords = useQuery({ ...popularKeywordsQuery(), enabled: tab === 'keyword' });

  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>{query ? `${query} 검색 결과 | shout-outz` : '탐색 | shout-outz'}</title>
      <AppGnb />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 pt-12 pb-18 break-keep md:pt-20 md:pb-28">
        <ExploreHero
          tab={tab}
          onTabChange={onTabChange}
          query={query}
          onSearch={onSearch}
          {...(tab === 'keyword' &&
            popularKeywords.data && {
              suggestions: popularKeywords.data.map(({ keyword }) => keyword),
            })}
          {...(tab === 'project' && {
            activeSuggestion: topic,
            // 프로젝트 탭의 추천 검색어는 "이런 주제를 찾아보세요"라 검색어 대신 기술 주제 필터로 건다. 같은 것을 다시 누르면 푼다.
            onSuggestionSelect: (suggestion: string) =>
              onTopicChange(suggestion === topic ? null : suggestion),
          })}
        />
        {tab === 'project' ? (
          <ProjectExplore
            filter={{ keyword: query, topic, sort }}
            onTopicChange={onTopicChange}
            onSortChange={onSortChange}
          />
        ) : tab === 'job' ? (
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
