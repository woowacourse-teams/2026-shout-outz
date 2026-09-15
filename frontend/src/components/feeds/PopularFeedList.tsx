import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { feedsQuery } from '@/apis/feed';
import { Badge } from '@/components/Badge';
import { FeedAuthor } from '@/components/feeds/FeedAuthor';

export function PopularFeedList() {
  const { data } = useSuspenseInfiniteQuery(feedsQuery('POPULAR', undefined, 3));
  const feeds = data.pages[0]?.data ?? [];

  return (
    <aside aria-label="이번 주 인기 피드">
      <div className="mb-5 flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900">이번 주 인기 피드</h2>
        <Badge tone="primary">TOP 3</Badge>
      </div>
      <ol className="divide-y divide-gray-100">
        {feeds.map((feed) => (
          <li key={feed.postId} className="py-4 first:pt-0">
            <FeedAuthor feed={feed} compact />
            <p className="mt-2 line-clamp-2 text-sm leading-5 text-gray-600">“{feed.content}”</p>
          </li>
        ))}
      </ol>
    </aside>
  );
}
