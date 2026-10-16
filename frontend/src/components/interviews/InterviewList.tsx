import { useEffect, useRef } from 'react';
import { IconMicrophone } from '@tabler/icons-react';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { interviewsQuery } from '@/apis/interview';
import { Button } from '@/components/Button';
import { InterviewCard } from '@/components/interviews/InterviewCard';
import { getApiErrorMessage } from '@/utils/error';

export function InterviewList() {
  const query = useSuspenseInfiniteQuery(interviewsQuery());
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

  const interviews = query.data.pages.flatMap((page) => page.data);

  if (interviews.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-16 text-center text-gray-500">
        <IconMicrophone className="size-8 text-gray-300" aria-hidden="true" />
        <p>아직 공개된 인터뷰가 없습니다.</p>
      </div>
    );
  }

  return (
    <div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {interviews.map((interview) => (
          <InterviewCard key={interview.id} interview={interview} />
        ))}
      </div>
      {isFetchNextPageError && (
        <p role="alert" className="py-3 text-sm text-red-600">
          {getApiErrorMessage(query.error)}
        </p>
      )}
      <div ref={sentinel} />
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
                ? '추가 인터뷰 다시 시도'
                : '인터뷰 더 보기'}
          </Button>
        </div>
      )}
    </div>
  );
}
