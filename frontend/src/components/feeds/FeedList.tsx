import { Fragment, useEffect, useRef } from 'react';
import { IconChevronDown, IconMessageCircle } from '@tabler/icons-react';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { feedsQuery, type FeedSort, type FeedType } from '@/apis/feed';
import { Button } from '@/components/Button';
import { RecommendationCard } from '@/components/recommendations/RecommendationCard';
import { FeedListItem } from '@/components/feeds/FeedListItem';
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
  // 목록 전체에서 추천 카드가 놓인 순서(0부터). 페이지를 더 불러와도 앞 카드의 순서는 그대로다.
  const slotPositions = new Map(
    feedType === 'QUESTION'
      ? query.data.pages
          .flatMap((page) =>
            page.data.flatMap((feed) =>
              (page.meta.recommendationSlots ?? []).filter(
                (slot) => slot.afterFeedId === feed.feedId,
              ),
            ),
          )
          .map((slot, index) => [slot, index] as const)
      : [],
  );

  return (
    <div>
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
        <ul aria-label={itemLabel}>
          {query.data.pages.map((page, pageIndex) => (
            <Fragment key={pageIndex}>
              {page.data.map((feed) => (
                <Fragment key={feed.feedId}>
                  <li className="border-t border-gray-100 py-6">
                    <FeedListItem feed={feed} surface="feeds" />
                  </li>
                  {feedType === 'QUESTION' &&
                    page.meta.recommendationSlots
                      ?.filter((slot) => slot.afterFeedId === feed.feedId)
                      .map((slot) => (
                        // PROTOTYPE-DIFF: 추천 질문 카드는 글 상세의 관련 질문에서도 써서 기존 카드를 그대로 둔다.
                        // 프로토타입은 테두리 없는 파란 배경, 큰 제목, 본문 없음, "이 질문 이어서 하기 →" 문구다.
                        <li key={slot.recommendation.recommendationId} className="py-2">
                          <RecommendationCard
                            recommendation={slot.recommendation}
                            surface="feed_list"
                            position={slotPositions.get(slot) ?? 0}
                          />
                        </li>
                      ))}
                </Fragment>
              ))}
            </Fragment>
          ))}
        </ul>
      )}
      {isFetchNextPageError && (
        <p role="alert" className="py-3 text-sm text-red-600">
          {getApiErrorMessage(query.error)}
        </p>
      )}
      <div ref={sentinel} />
      {hasNextPage && (
        <div className="mt-5 flex justify-center">
          <Button
            variant="outline"
            size="sm"
            className="rounded-full"
            disabled={isFetchingNextPage}
            onClick={() => void fetchNextPage({ cancelRefetch: false })}
          >
            {isFetchingNextPage
              ? '불러오는 중…'
              : isFetchNextPageError
                ? `추가 ${itemLabel} 다시 시도`
                : `${itemLabel} 더 보기`}
            {!isFetchingNextPage && !isFetchNextPageError && (
              <IconChevronDown className="ml-1 size-3.5" aria-hidden="true" />
            )}
          </Button>
        </div>
      )}
    </div>
  );
}
