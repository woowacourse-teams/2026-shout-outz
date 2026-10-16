import { Fragment, useEffect, useRef } from 'react';
import { IconMessageCircle } from '@tabler/icons-react';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { feedsQuery, type FeedSort, type FeedType } from '@/apis/feed';
import { Button } from '@/components/Button';
import { RecommendationCard } from '@/components/recommendations/RecommendationCard';
import { FeedCard } from '@/components/feeds/FeedCard';
import { getApiErrorMessage } from '@/utils/error';

export function FeedList({ sort, feedType }: { sort: FeedSort; feedType?: FeedType }) {
  const itemLabel = feedType === 'QUESTION' ? '질문' : '피드';
  const query = useSuspenseInfiniteQuery({
    ...feedsQuery(sort, undefined, 20, feedType),
    // 무작위 슬롯을 이미 읽은 페이지에서 포커스/재진입만으로 다시 뽑지 않습니다.
    refetchOnWindowFocus: false,
    refetchOnMount: false,
    refetchOnReconnect: false,
  });
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

  const feeds = query.data.pages.flatMap((page) => page.data);

  return (
    <div className="space-y-3">
      {feeds.length === 0 ? (
        <div className="flex flex-col items-center gap-3 py-16 text-center text-gray-500">
          <IconMessageCircle className="size-8 text-gray-300" aria-hidden="true" />
          <p>
            {feedType === 'QUESTION'
              ? sort === 'WAITING'
                ? '답변을 기다리는 질문이 없어요!'
                : '아직 등록된 질문이 없습니다.'
              : '아직 등록된 피드가 없습니다.'}
          </p>
        </div>
      ) : (
        query.data.pages.map((page, pageIndex) => (
          <Fragment key={pageIndex}>
            {page.data.map((feed) => (
              <Fragment key={feed.feedId}>
                <FeedCard feed={feed} surface="feeds" />
                {feedType === 'QUESTION' &&
                  page.meta.recommendationSlots
                    ?.filter((slot) => slot.afterFeedId === feed.feedId)
                    .map((slot) => (
                      <RecommendationCard
                        key={slot.recommendation.recommendationId}
                        recommendation={slot.recommendation}
                      />
                    ))}
              </Fragment>
            ))}
          </Fragment>
        ))
      )}
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
                ? `추가 ${itemLabel} 다시 시도`
                : `${itemLabel} 더 보기`}
          </Button>
        </div>
      )}
    </div>
  );
}
