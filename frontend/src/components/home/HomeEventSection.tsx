import { useId } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';

import { newsListQueryOptions } from '@/api/news';
import { NewsItem } from '@/components/NewsItem';

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
          진행 중인 크루 이벤트
        </h2>

        <Link
          to="/news"
          className="text-primary-600 focus-visible:outline-primary-600 shrink-0 rounded-sm text-xs font-bold focus-visible:outline-2"
        >
          소식 더보기 ›
        </Link>
      </div>

      {events.length === 0 ? (
        <p className="py-8 text-center text-sm text-gray-500">진행 중인 이벤트가 없습니다.</p>
      ) : (
        <ul className="flex flex-col gap-4">
          {events.map(({ id, ...event }) => (
            <li key={id} className="border-b border-gray-100 pb-4 last:border-b-0 last:pb-0">
              <Link
                to="/news/$newsId"
                params={{ newsId: String(id) }}
                className="focus-visible:outline-primary-600 block rounded-sm focus-visible:outline-2"
              >
                <NewsItem {...event} />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
