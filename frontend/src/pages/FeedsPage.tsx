import type { FeedSort } from '@/apis/feed';
import { Gnb } from '@/components/Gnb';
import { Tab } from '@/components/Tab';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { FeedList } from '@/components/feeds/FeedList';
import { FeedFooter } from '@/components/feeds/FeedFooter';
import { PopularFeedList } from '@/components/feeds/PopularFeedList';
import { useMediaQuery } from '@/hooks/useMediaQuery';

const DESKTOP_MEDIA_QUERY = '(min-width: 64rem)';

export function FeedsPage({
  sort,
  onSortChange,
}: {
  sort: FeedSort;
  onSortChange: (sort: FeedSort) => void;
}) {
  const isDesktop = useMediaQuery(DESKTOP_MEDIA_QUERY);

  return (
    <div className="bg-background flex min-h-dvh flex-col">
      <Gnb />
      <main className="mx-auto grid w-full max-w-6xl flex-1 grid-cols-1 px-4 py-6 md:px-16 md:py-10 lg:grid-cols-3 lg:gap-12">
        <section className="min-w-0 lg:col-span-2">
          <h1 className="text-2xl font-bold text-gray-900">피드</h1>
          <div className="flex flex-col gap-4">
            <div className="mt-6">
              <Tab
                variant="chip"
                size="sm"
                value={sort}
                onChange={(value) => onSortChange(value as FeedSort)}
                aria-label="피드 정렬"
              >
                <Tab.Item value="LATEST">최신순</Tab.Item>
                <Tab.Item value="POPULAR">인기순</Tab.Item>
              </Tab>
            </div>
            <AsyncBoundary key={sort}>
              <FeedList sort={sort} />
            </AsyncBoundary>
          </div>
        </section>
        {isDesktop && (
          <div className="min-w-0">
            <AsyncBoundary>
              <PopularFeedList />
            </AsyncBoundary>
          </div>
        )}
      </main>
      <FeedFooter />
    </div>
  );
}
