import { useEffect, useRef } from 'react';
import { IconMessageCircle } from '@tabler/icons-react';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { feedsQuery, type FeedSort, type FeedType } from '@/apis/feed';
import { Button } from '@/components/Button';
import { FeedCard } from '@/components/feeds/FeedCard';
import { getApiErrorMessage } from '@/utils/error';

export function FeedList({ sort, feedType }: { sort: FeedSort; feedType?: FeedType }) {
  const itemLabel = feedType === 'QUESTION' ? '질문' : '피드';
  const query = useSuspenseInfiniteQuery(feedsQuery(sort, undefined, 20, feedType));
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
              ? '아직 등록된 질문이 없습니다.'
              : '아직 등록된 피드가 없습니다.'}
          </p>
        </div>
      ) : (
        feeds.map((feed) => <FeedCard key={feed.feedId} feed={feed} surface="feeds" />)
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
