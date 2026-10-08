import { useSuspenseQuery } from '@tanstack/react-query';
import { getRouteApi, Link } from '@tanstack/react-router';
import { IconArrowLeft, IconClock } from '@tabler/icons-react';

import { NewsEventPeriod } from '@/components/NewsEventPeriod';
import { NewsEventStatusBadge } from '@/components/NewsEventStatusBadge';
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
      <main className="mx-auto w-full max-w-4xl flex-1 px-5 pt-7 pb-16 md:px-12 md:pt-8 md:pb-20">
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
    <article className="min-w-0">
      <title>{`${news.title} | shout-outz`}</title>
      <Link
        to="/news"
        className="mb-7 inline-flex items-center gap-2 text-xs text-gray-500 hover:text-gray-900"
      >
        <IconArrowLeft className="size-4" aria-hidden="true" />
        목록으로
      </Link>
      <div className="flex items-center gap-2">
        <NewsCategoryBadge type={news.type} />
      </div>
      <h1 className="mt-3 text-2xl leading-snug font-bold tracking-tight break-words text-gray-900 md:text-3xl">
        {news.title}
      </h1>
      <div className="mt-7 flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-gray-500">
        <span className="font-medium text-gray-900">{news.author.name}</span>
        <span aria-hidden="true">·</span>
        <time dateTime={news.publishedAt}>{formatDotDate(news.publishedAt)}</time>
      </div>

      {news.type === 'EVENT' && (news.eventStatus || news.eventStartAt || news.eventEndAt) && (
        <section aria-label="이벤트 일정" className="mt-6 rounded-lg bg-gray-50 p-4 md:p-5">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-x-3 gap-y-2">
            <h2 className="inline-flex items-center gap-2 text-sm font-semibold text-gray-900">
              <IconClock aria-hidden="true" className="size-4 text-gray-500" />
              이벤트 일정
            </h2>
            <NewsEventStatusBadge status={news.eventStatus} />
          </div>
          <NewsEventPeriod startAt={news.eventStartAt} endAt={news.eventEndAt} />
        </section>
      )}

      <div className="mt-7 border-t border-gray-100 pt-8 text-sm leading-7 break-words whitespace-pre-line text-gray-600 md:text-base">
        {news.body}
      </div>

      {news.cta && (
        <a
          href={news.cta.url}
          className="bg-primary-500 hover:bg-primary-600 focus-visible:ring-primary-600 mt-7 inline-flex min-h-10 items-center justify-center rounded-lg px-5 text-sm font-medium text-white focus-visible:ring-2 focus-visible:outline-none"
        >
          {news.cta.label}
        </a>
      )}

      {(news.previous || news.next) && (
        <nav aria-label="이전 다음 소식" className="mt-12 flex flex-col border-t border-gray-200">
          {news.previous && <NewsNavRow direction="previous" {...news.previous} />}
          {news.next && <NewsNavRow direction="next" {...news.next} />}
        </nav>
      )}
    </article>
  );
}
