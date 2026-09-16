import { useEffect, useRef } from 'react';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { feedsQuery, type FeedSort } from '@/apis/feed';
import { Button } from '@/components/Button';
import { FeedCard } from '@/components/feeds/FeedCard';
import { getApiErrorMessage } from '@/utils/error';

export function FeedList({ sort }: { sort: FeedSort }) {
  const query = useSuspenseInfiniteQuery(feedsQuery(sort));
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
    <div>
      {feeds.length === 0 ? (
        <p className="py-12 text-center text-gray-500">아직 등록된 피드가 없습니다.</p>
      ) : (
        feeds.map((feed) => <FeedCard key={feed.feedId} feed={feed} />)
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
                ? '추가 피드 다시 시도'
                : '피드 더 보기'}
          </Button>
        </div>
      )}
    </div>
  );
}
