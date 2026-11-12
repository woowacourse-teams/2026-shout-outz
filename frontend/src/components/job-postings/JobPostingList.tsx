import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';

import { jobPostingsQuery } from '@/apis/job-posting';
import { Button } from '@/components/Button';
import { JobPostingCard, UpcomingJobPostingCard } from '@/components/job-postings/JobPostingCard';
import { getApiErrorMessage } from '@/utils/error';

/** 탐색 화면 채용공고 탭의 공고 카드 목록. 데스크톱 3열, 모바일 1열 */
export function JobPostingList({ keyword }: { keyword: string }) {
  const query = useSuspenseInfiniteQuery(jobPostingsQuery(keyword));
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

  if (keyword && jobPostings.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-gray-200 px-4 py-10 text-center">
        <p className="font-semibold text-gray-900">
          &apos;{keyword}&apos;에 맞는 공고를 아직 못 찾았어요.
        </p>
        <p className="mt-2 text-sm text-gray-500">회사, 직무, 기술 이름으로 다시 찾아보세요.</p>
      </div>
    );
  }

  return (
    <div>
      <ul aria-label="공고" className="grid gap-3 md:grid-cols-3 md:gap-4">
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
