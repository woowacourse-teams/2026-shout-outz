import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';

import { jobPostingsQuery, type JobPostingSort } from '@/apis/job-posting';
import { Button } from '@/components/Button';
import { JobPostingCard, UpcomingJobPostingCard } from '@/components/job-postings/JobPostingCard';
import { analytics } from '@/utils/analytics';
import { cn } from '@/utils/cn';
import { getApiErrorMessage } from '@/utils/error';

// PROTOTYPE-DIFF: 프로토타입은 마감 임박순이 기본이지만, 서버에 아직 없어 최신 등록순을 기본으로 둔다(routes/index.tsx).
const SORT_OPTIONS: { sort: JobPostingSort; label: string }[] = [
  { sort: 'DEADLINE', label: '마감 임박순' },
  { sort: 'LATEST', label: '최신 등록순' },
];

interface JobPostingListProps {
  keyword: string;
  sort: JobPostingSort;
  onSortChange: (sort: JobPostingSort) => void;
}

/** 탐색 화면 채용공고 탭의 공고 목록. 머리에 공고 수와 정렬, 아래에 카드(데스크톱 3열, 모바일 1열) */
export function JobPostingList({ keyword, sort, onSortChange }: JobPostingListProps) {
  const query = useSuspenseInfiniteQuery(jobPostingsQuery(keyword, sort));
  const sentinel = useRef<HTMLDivElement>(null);
  const { fetchNextPage, hasNextPage, isFetchingNextPage, isFetchNextPageError } = query;

  useEffect(() => {
    if (!sentinel.current || !hasNextPage || isFetchingNextPage) return;
    if (isFetchNextPageError || typeof IntersectionObserver === 'undefined') return;

    const observer = new IntersectionObserver(([entry]) => {
      if (entry?.isIntersecting) void fetchNextPage({ cancelRefetch: false });
    });
    observer.observe(sentinel.current);

    return () => observer.disconnect();
  }, [fetchNextPage, hasNextPage, isFetchingNextPage, isFetchNextPageError]);

  const jobPostings = query.data.pages.flatMap((page) => page.data);
  const totalCount = query.data.pages[0]?.meta?.totalCount ?? jobPostings.length;

  const header = (
    <div className="flex items-end justify-between gap-4">
      <div className="min-w-0">
        <h2 className="text-xl font-bold break-keep text-gray-900">
          {keyword ? `‘${keyword}’ 검색 결과` : '채용공고'}{' '}
          <span className="font-medium text-gray-400">{totalCount}</span>
        </h2>
        {/* 모바일에서는 설명을 빼고 공고 수와 정렬만 둔다. */}
        <p className="mt-1 hidden text-sm text-gray-500 md:block">
          공고마다 크루들의 경험으로 찾는 역량을 정리했어요.
        </p>
      </div>
      <div
        role="group"
        aria-label="정렬"
        className="flex shrink-0 items-center text-xs text-gray-500"
      >
        {SORT_OPTIONS.map((option, index) => (
          <span key={option.sort} className="flex items-center">
            {index > 0 && (
              <span aria-hidden="true" className="px-1">
                ·
              </span>
            )}
            <button
              type="button"
              aria-pressed={sort === option.sort}
              onClick={() => {
                analytics.track({ name: 'job_sort_changed', sort: option.sort });
                onSortChange(option.sort);
              }}
              className={cn(
                'focus-visible:outline-primary-600 cursor-pointer rounded-sm focus-visible:outline-2',
                sort === option.sort ? 'font-bold text-gray-900' : 'hover:text-gray-700',
              )}
            >
              {option.label}
            </button>
          </span>
        ))}
      </div>
    </div>
  );

  if (keyword && jobPostings.length === 0) {
    return (
      <div>
        {header}
        <div className="mt-4 rounded-2xl border border-dashed border-gray-200 px-4 py-10 text-center">
          <p className="font-semibold text-gray-900">
            &apos;{keyword}&apos;에 맞는 공고를 아직 못 찾았어요.
          </p>
          <p className="mt-2 text-sm text-gray-500">회사, 직무, 기술 이름으로 다시 찾아보세요.</p>
        </div>
      </div>
    );
  }

  return (
    <div>
      {header}
      <ul aria-label="공고" className="mt-4 grid gap-3 md:grid-cols-3 md:gap-4">
        {jobPostings.map((jobPosting, position) => (
          <li key={jobPosting.jobPostingId}>
            <JobPostingCard jobPosting={jobPosting} position={position} />
          </li>
        ))}
        {!keyword && !hasNextPage && (
          <li>
            <UpcomingJobPostingCard />
          </li>
        )}
      </ul>
      {isFetchNextPageError && (
        <p role="alert" className="py-3 text-sm text-red-600">
          {getApiErrorMessage(query.error)}
        </p>
      )}
      <div ref={sentinel} />
      {/* 매거진 목록처럼 스크롤 끝에서 다음 페이지를 이어 불러오고, 실패하면 버튼으로 다시 시도한다. */}
      {hasNextPage && (
        <div className="mt-5 grid">
          <Button
            variant="outline"
            disabled={isFetchingNextPage}
            onClick={() => void fetchNextPage({ cancelRefetch: false })}
          >
            {isFetchingNextPage
              ? '불러오는 중…'
              : isFetchNextPageError
                ? '공고 다시 불러오기'
                : '공고 더 보기'}
          </Button>
        </div>
      )}
    </div>
  );
}
