import type { FeedSort, FeedType } from '@/apis/feed';
import { IconMessage, IconMessageQuestion, IconPlus } from '@tabler/icons-react';
import { Button } from '@/components/Button';
import { Footer } from '@/components/Footer';
import { AppGnb } from '@/components/AppGnb';
import { Tab } from '@/components/Tab';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { FeedList } from '@/components/feeds/FeedList';
import { PopularFeedList } from '@/components/feeds/PopularFeedList';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { analytics } from '@/utils/analytics';

const DESKTOP_MEDIA_QUERY = '(min-width: 64rem)';

export function FeedsPage({
  sort,
  feedType,
  onSortChange,
  onTypeChange,
  onCreate,
}: {
  sort: FeedSort;
  feedType: FeedType;
  onCreate: () => void;
  onSortChange: (sort: FeedSort) => void;
  onTypeChange: (type: FeedType) => void;
}) {
  const isDesktop = useMediaQuery(DESKTOP_MEDIA_QUERY);
  const isQuestion = feedType === 'QUESTION';
  const sidebarFeedType: FeedType = isQuestion ? 'POST' : 'QUESTION';

  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>커뮤니티 | shout-outz</title>
      <AppGnb />
      <main className="mx-auto grid w-full max-w-6xl flex-1 grid-cols-1 gap-10 px-4 pt-6 pb-12 md:pt-10 md:pb-20 lg:grid-cols-[minmax(0,1fr)_22rem] xl:grid-cols-[minmax(0,1fr)_24rem]">
        <section className="min-w-0">
          <div className="flex items-center justify-between gap-4">
            <h1 className="text-xl font-bold tracking-tight text-gray-900 md:text-2xl">커뮤니티</h1>
            <Button variant="primary" size="sm" className="shrink-0" onClick={onCreate}>
              <IconPlus className="mr-1 size-3" aria-hidden="true" />
              {isQuestion ? '질문하기' : '글쓰기'}
            </Button>
          </div>
          <Tab
            variant="subnav"
            size="md"
            className="mt-5 w-full border-b border-gray-200"
            value={feedType}
            onChange={(value) => onTypeChange(value as FeedType)}
            aria-label="커뮤니티 유형"
          >
            <Tab.Item value="QUESTION">
              <IconMessageQuestion className="mr-1.5 size-4" aria-hidden="true" />
              질문
            </Tab.Item>
            <Tab.Item value="POST">
              <IconMessage className="mr-1.5 size-4" aria-hidden="true" />
              피드
            </Tab.Item>
          </Tab>
          <div className="flex flex-col gap-3">
            <div className="mt-5 border-b border-gray-200 pb-3">
              <Tab
                variant="chip"
                className="flex-wrap"
                size="sm"
                value={sort}
                onChange={(value) => {
                  analytics.track({ name: 'feed_sort_changed', sort: value, surface: 'feeds' });
                  onSortChange(value as FeedSort);
                }}
                aria-label="피드 정렬"
              >
                <Tab.Item value="LATEST">최신순</Tab.Item>
                <Tab.Item value="POPULAR">인기순</Tab.Item>
                {isQuestion && <Tab.Item value="WAITING">답변을 기다리고 있어요</Tab.Item>}
              </Tab>
            </div>
            <AsyncBoundary key={`${feedType}-${sort}`}>
              <FeedList sort={sort} feedType={feedType} />
            </AsyncBoundary>
          </div>
        </section>
        {isDesktop && (
          <div className="min-w-0 self-start">
            <AsyncBoundary key={sidebarFeedType}>
              <PopularFeedList feedType={sidebarFeedType} />
            </AsyncBoundary>
          </div>
        )}
      </main>
      <Footer />
    </div>
  );
}
