import { useId } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';

import { newsListQueryOptions } from '@/api/news';
import { NewsItem } from '@/components/NewsItem';
import { getButtonStyles } from '@/components/Button';
import { analytics } from '@/utils/analytics';

const HOME_EVENT_SIZE = 2;

export function HomeEventSection() {
  const headingId = useId();
  const { data: events } = useSuspenseQuery(
    newsListQueryOptions('EVENT', 'LATEST', { eventStatus: 'ONGOING', size: HOME_EVENT_SIZE }),
  );

  return (
    <section aria-labelledby={headingId} className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-2 pb-1">
        <h2 id={headingId} className="text-base font-bold tracking-tight text-gray-900">
          진행 중인 이벤트
        </h2>
      </div>

      {events.length === 0 ? (
        <p className="py-8 text-center text-sm text-gray-500">진행 중인 이벤트가 없습니다.</p>
      ) : (
        <ul className="flex flex-col gap-3">
          {events.map(
            ({ id, type, title, summary, publishedAt, eventStatus, eventStartAt, eventEndAt }) => (
              <li key={id} className="group min-w-0">
                <Link
                  to="/news/$newsId"
                  params={{ newsId: String(id) }}
                  className="bg-background focus-visible:outline-primary-600 block rounded-lg border border-gray-200 p-4 transition-colors hover:border-gray-300 focus-visible:outline-2 focus-visible:outline-offset-2"
                  onClick={() => {
                    analytics.track({ name: 'card_clicked', target: 'news', surface: 'home' });
                    analytics.track({
                      name: 'news_detail_opened',
                      newsId: id,
                      type,
                      from: 'home',
                    });
                  }}
                >
                  <NewsItem
                    type={type}
                    title={title}
                    summary={summary}
                    publishedAt={publishedAt}
                    eventStatus={eventStatus}
                    eventStartAt={eventStartAt}
                    eventEndAt={eventEndAt}
                    compact
                  />
                </Link>
              </li>
            ),
          )}
        </ul>
      )}
      <Link
        to="/news"
        search={{ type: 'EVENT', sort: 'LATEST' }}
        className={getButtonStyles({ variant: 'outline', className: 'w-full' })}
        onClick={() => analytics.track({ name: 'section_more_clicked', target: 'news' })}
      >
        이벤트 전체보기 ›
      </Link>
    </section>
  );
}
