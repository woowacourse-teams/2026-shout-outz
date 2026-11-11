import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { IconChevronDown } from '@tabler/icons-react';

import { exploreProjectsQuery, type ExploreProjectFilter } from '@/apis/project-explore';
import { ExploreProjectCard } from '@/components/project-explore/ExploreProjectCard';
import type { ProjectSort } from '@/types/project';
import { cn } from '@/utils/cn';

const SORT_OPTIONS: { sort: ProjectSort; label: string }[] = [
  { sort: 'LATEST', label: '최신 등록순' },
  // 프로젝트 목록 화면은 "인기순"이지만 프로토타입 문구를 따른다. 서버 정렬은 좋아요 수 기준이다.
  { sort: 'POPULAR', label: '좋아요순' },
];

interface ProjectExploreListProps {
  filter: ExploreProjectFilter;
  onSortChange: (sort: ProjectSort) => void;
}

export function ProjectExploreList({ filter, onSortChange }: ProjectExploreListProps) {
  const query = useSuspenseInfiniteQuery(exploreProjectsQuery(filter));
  const projects = query.data.pages.flatMap((page) => page.projects);
  const totalCount = query.data.pages[0]?.meta.totalCount ?? projects.length;
  // 검색어와 기술 주제는 함께 걸리지 않는다(routes/explore.tsx).
  const searchedBy = filter.topic ?? filter.keyword.trim();

  return (
    <div>
      <div className="flex items-end justify-between gap-4">
        <div className="min-w-0">
          <h2 className="text-xl font-bold text-gray-900">
            프로젝트 <span className="font-medium text-gray-400">{totalCount}</span>
          </h2>
          {searchedBy && (
            <p className="mt-1 text-sm break-keep text-gray-500">
              <b className="font-semibold text-gray-900">{searchedBy}</b> 키워드로 찾은 결과
            </p>
          )}
        </div>
        <div
          role="group"
          aria-label="정렬"
          className="flex shrink-0 items-center text-sm text-gray-500"
        >
          {SORT_OPTIONS.map(({ sort, label }, index) => (
            <span key={sort} className="flex items-center">
              {index > 0 && (
                <span aria-hidden="true" className="px-1">
                  ·
                </span>
              )}
              <button
                type="button"
                aria-pressed={filter.sort === sort}
                onClick={() => onSortChange(sort)}
                className={cn(
                  'focus-visible:outline-primary-600 cursor-pointer rounded-sm focus-visible:outline-2',
                  filter.sort === sort ? 'font-bold text-gray-900' : 'hover:text-gray-700',
                )}
              >
                {label}
              </button>
            </span>
          ))}
        </div>
      </div>

      {projects.length === 0 ? (
        <p className="py-16 text-center text-gray-500">
          {filter.keyword
            ? `'${filter.keyword}'에 맞는 프로젝트를 아직 못 찾았어요.`
            : '고른 주제를 다룬 프로젝트를 아직 못 찾았어요.'}
        </p>
      ) : (
        <ul
          aria-label="프로젝트"
          className="mt-5 grid grid-cols-1 gap-x-5 gap-y-8 sm:grid-cols-2 lg:grid-cols-3"
        >
          {projects.map((project) => (
            <li key={project.slug} className="min-w-0">
              <ExploreProjectCard {...project} />
            </li>
          ))}
        </ul>
      )}

      {query.hasNextPage && (
        <div className="mt-8 flex justify-center">
          <button
            type="button"
            disabled={query.isFetchingNextPage}
            onClick={() => void query.fetchNextPage({ cancelRefetch: false })}
            className="focus-visible:outline-primary-600 inline-flex h-8.5 cursor-pointer items-center gap-1 rounded-full border border-gray-200 px-3.5 text-sm font-semibold text-gray-600 hover:bg-gray-50 focus-visible:outline-2 disabled:cursor-default disabled:opacity-50"
          >
            {query.isFetchingNextPage ? '불러오는 중…' : '더 보기'}
            <IconChevronDown className="size-3.5" aria-hidden="true" />
          </button>
        </div>
      )}
    </div>
  );
}
