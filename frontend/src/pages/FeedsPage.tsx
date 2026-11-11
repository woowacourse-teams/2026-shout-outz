import type { FeedSort, FeedType } from '@/apis/feed';
import { IconPlus } from '@tabler/icons-react';
import { Button } from '@/components/Button';
import { Footer } from '@/components/Footer';
import { AppGnb } from '@/components/AppGnb';
import { Tab } from '@/components/Tab';
import { DotTab } from '@/components/explore/DotTab';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { FeedList } from '@/components/feeds/FeedList';
import { PopularFeedList } from '@/components/feeds/PopularFeedList';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { analytics } from '@/utils/analytics';

const DESKTOP_MEDIA_QUERY = '(min-width: 64rem)';

const SORT_OPTIONS: { value: FeedSort; label: string }[] = [
  { value: 'LATEST', label: '최신순' },
  { value: 'POPULAR', label: '인기순' },
  { value: 'WAITING', label: '답변을 기다리고 있어요' },
];

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
      <main className="mx-auto grid w-full max-w-6xl flex-1 grid-cols-1 gap-12 px-6 pt-10 pb-30 lg:grid-cols-[minmax(0,1fr)_17rem]">
        <section className="min-w-0">
          <div className="flex items-end justify-between gap-4 pb-6">
            <div className="flex min-w-0 flex-col gap-2">
              <h1 className="text-3xl font-extrabold tracking-tight text-gray-900">커뮤니티</h1>
              <p className="text-base text-gray-600">
                궁금한 건 편하게 묻고, 서로의 이야기에 응원을 보내 보세요.
              </p>
            </div>
            <Button
              size="md"
              className="shrink-0 bg-gray-900 px-4 font-bold text-white hover:bg-gray-700"
              onClick={onCreate}
            >
              <IconPlus className="mr-1.5 size-4" aria-hidden="true" />
              {isQuestion ? '질문하기' : '글쓰기'}
            </Button>
          </div>
          <Tab
            variant="underline"
            size="md"
            className="w-full border-b border-gray-200"
            value={feedType}
            onChange={(value) => {
              analytics.track({
                name: 'feed_type_changed',
                feedType: value as FeedType,
                surface: 'feeds',
              });
              onTypeChange(value as FeedType);
            }}
            aria-label="커뮤니티 유형"
          >
            <Tab.Item value="QUESTION">질문</Tab.Item>
            <Tab.Item value="POST">피드</Tab.Item>
          </Tab>
          <div role="tablist" aria-label="피드 정렬" className="flex flex-wrap gap-4.5 pt-4 pb-1">
            {SORT_OPTIONS.filter((option) => isQuestion || option.value !== 'WAITING').map(
              (option) => (
                <DotTab
                  key={option.value}
                  selected={sort === option.value}
                  onSelect={() => {
                    if (sort === option.value) return;
                    analytics.track({
                      name: 'feed_sort_changed',
                      sort: option.value,
                      surface: 'feeds',
                      feedType,
                    });
                    onSortChange(option.value);
                  }}
                >
                  {option.label}
                </DotTab>
              ),
            )}
          </div>
          <div className="mt-3">
            <AsyncBoundary key={`${feedType}-${sort}`}>
              <FeedList sort={sort} feedType={feedType} />
            </AsyncBoundary>
          </div>
        </section>
        {isDesktop && (
          <div className="min-w-0 self-start lg:sticky lg:top-28">
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
