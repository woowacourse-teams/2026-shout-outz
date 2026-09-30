import { useSuspenseQuery } from '@tanstack/react-query';
import { getRouteApi } from '@tanstack/react-router';

import { NewsCategoryBadge } from '@/components/NewsCategoryBadge';
import { NewsNavRow } from '@/components/NewsNavRow';
import { AppGnb } from '@/components/AppGnb';
import { Footer } from '@/components/Footer';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Button } from '@/components/Button';
import { newsDetailQueryOptions } from '@/api/news';
import { formatDotDate } from '@/utils/date';
import { isHTTPError } from 'ky';

const route = getRouteApi('/news/$newsId');

export function NewsDetailPage() {
  const { newsId } = route.useParams();

  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <AppGnb aria-label="주요 헤더" />
      <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-5 px-4 pt-6 pb-12 md:gap-7 md:pt-10 md:pb-20">
        <AsyncBoundary
          key={newsId}
          fallback={
            <p role="status" className="py-16 text-center text-sm text-gray-500">
              소식을 불러오는 중…
            </p>
          }
          errorFallback={(error, reset) => {
            const unavailable = isHTTPError(error) && error.response.status === 404;
            return (
              <div role="alert" className="space-y-4 py-16 text-center">
                <title>소식 조회 오류 | shout-outz</title>
                <h1 className="text-xl font-bold">
                  {unavailable
                    ? '소식이 없거나 접근할 수 없습니다.'
                    : '소식을 불러오지 못했습니다.'}
                </h1>
                {!unavailable && (
                  <Button variant="outline" onClick={reset}>
                    다시 시도
                  </Button>
                )}
              </div>
            );
          }}
        >
          <NewsDetail newsId={Number(newsId)} />
        </AsyncBoundary>
      </main>
      <Footer />
    </div>
  );
}

function NewsDetail({ newsId }: { newsId: number }) {
  const { data: news } = useSuspenseQuery(newsDetailQueryOptions(newsId));

  return (
    <>
      <title>{`${news.title} | shout-outz`}</title>
      <div className="flex flex-col gap-2 md:gap-3">
        <div className="flex items-center gap-1.5 text-xs text-gray-500 md:gap-2 md:text-sm">
          <NewsCategoryBadge type={news.type} />
          <time dateTime={news.publishedAt}>{formatDotDate(news.publishedAt)}</time>
          <span className="md:text-gray-600">· {news.author.name}</span>
        </div>

        <h1 className="text-xl leading-snug font-bold tracking-tight text-gray-900 md:text-2xl">
          {news.title}
        </h1>
      </div>

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
    </>
  );
}
