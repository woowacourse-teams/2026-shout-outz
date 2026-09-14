import { useSuspenseQuery } from '@tanstack/react-query';
import { getRouteApi } from '@tanstack/react-router';

import { NewsCategoryBadge } from '@/components/NewsCategoryBadge';
import { NewsNavRow } from '@/components/NewsNavRow';
import { newsDetailQueryOptions } from '@/api/news';
import { formatDotDate } from '@/utils/date';

const route = getRouteApi('/news/$newsId');

export function NewsDetailPage() {
  const { newsId } = route.useParams();
  const { data: news } = useSuspenseQuery(newsDetailQueryOptions(Number(newsId)));

  return (
    <main className="flex flex-col gap-5 px-4 pt-5 pb-7 md:gap-7 md:px-16 md:pt-10 md:pb-20">
      <header className="flex flex-col gap-2 md:gap-3">
        <div className="flex items-center gap-1.5 text-xs text-gray-500 md:gap-2 md:text-sm">
          <NewsCategoryBadge type={news.type} />
          <time dateTime={news.publishedAt}>{formatDotDate(news.publishedAt)}</time>
          <span className="md:text-gray-600">· {news.author.name}</span>
        </div>

        <h1 className="text-lg leading-snug font-bold tracking-tight text-gray-900 md:text-2xl">
          {news.title}
        </h1>
      </header>

      {/* TODO body 형식 논의 필요 */}
      <p className="text-sm leading-relaxed whitespace-pre-line text-gray-900">{news.body}</p>

      {/* TODO CTA 형식 논의 필요 */}
      {news.cta && (
        <a
          href={news.cta.url}
          className="bg-primary-600 flex h-11 items-center justify-center rounded-lg px-6 text-sm font-bold text-white md:h-auto md:self-start md:py-3"
        >
          {news.cta.label}
        </a>
      )}

      {(news.previous || news.next) && (
        <nav aria-label="이전 다음 소식" className="flex flex-col">
          {news.previous && <NewsNavRow direction="previous" {...news.previous} />}
          {news.next && <NewsNavRow direction="next" {...news.next} />}
        </nav>
      )}
    </main>
  );
}
