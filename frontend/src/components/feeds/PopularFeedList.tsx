import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { feedsQuery } from '@/apis/feed';
import { Avatar } from '@/components/Avatar';
import { Badge } from '@/components/Badge';
import { toPlainText } from '@/utils/markdown';
import { formatCrewName } from '@/utils/user';

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
        {feeds.map((feed) => {
          const excerpt = toPlainText(feed.content);
          return (
            <li key={feed.feedId} className="py-4 first:pt-0">
              <Link
                to="/feeds/$feedId"
                params={{ feedId: String(feed.feedId) }}
                className="group focus-visible:outline-primary-600 block rounded-lg focus-visible:outline-2 focus-visible:outline-offset-4"
              >
                <p className="group-hover:text-primary-600 line-clamp-2 text-base leading-snug font-bold break-words text-gray-900 transition-colors">
                  {feed.title}
                </p>
                {excerpt && (
                  <p className="mt-1.5 line-clamp-2 text-sm leading-5 break-words text-gray-600">
                    {excerpt}
                  </p>
                )}
                <div className="mt-3 flex min-w-0 items-center gap-2">
                  <Avatar size="sm" alt={`${feed.author.displayName} 프로필`} />
                  <p className="truncate text-xs font-medium text-gray-500">
                    {formatCrewName(feed.author.displayName, feed.author.cohort, feed.author.track)}
                  </p>
                </div>
              </Link>
            </li>
          );
        })}
      </ol>
    </aside>
  );
}
