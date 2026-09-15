import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { feedsQuery } from '@/apis/feed';
import { Avatar } from '@/components/Avatar';
import { Badge } from '@/components/Badge';
import { getFeedAuthorName } from '@/utils/feed';

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
            <Link to="/feeds/$postId" params={{ postId: String(feed.postId) }}>
              <div className="flex min-w-0 items-center gap-2">
                <Avatar size="sm" alt={`${feed.author.displayName} 프로필`} />
                <p className="truncate text-sm font-semibold text-gray-900">
                  {getFeedAuthorName(feed.author)}
                </p>
              </div>
              <p className="mt-2 line-clamp-2 text-sm leading-5 text-gray-600">“{feed.content}”</p>
            </Link>
          </li>
        ))}
      </ol>
    </aside>
  );
}
