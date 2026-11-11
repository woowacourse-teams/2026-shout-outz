import { useSuspenseQuery } from '@tanstack/react-query';
import { getRouteApi, Link } from '@tanstack/react-router';
import { IconArrowLeft } from '@tabler/icons-react';

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
      <main className="mx-auto w-full max-w-3xl flex-1 px-6 pt-10 pb-28">
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
      <div className="flex items-center gap-3">
        <Link
          to="/news"
          className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-900"
        >
          <IconArrowLeft className="size-4" aria-hidden="true" />
          목록으로
        </Link>
        <NewsCategoryBadge type={news.type} className="px-2" />
      </div>
      <h1 className="mt-3 text-2xl leading-tight font-extrabold tracking-tight break-keep text-gray-900 md:text-3xl">
        {news.title}
      </h1>
      <p className="mt-3.5 text-xs text-gray-500">
        <b className="font-semibold text-gray-600">{news.author.name}</b>
        {' · '}
        <time dateTime={news.publishedAt}>{formatDotDate(news.publishedAt)}</time>
      </p>

      {news.type === 'EVENT' && (news.eventStatus || news.eventStartAt || news.eventEndAt) && (
        <section
          aria-label="이벤트 일정"
          className="mt-6 rounded-xl border border-gray-100 bg-gray-50 px-5 py-4.5"
        >
          <div className="mb-3 flex flex-wrap items-center justify-between gap-x-3 gap-y-2">
            <h2 className="text-sm font-bold text-gray-900">이벤트 일정</h2>
            <NewsEventStatusBadge status={news.eventStatus} variant="soft" />
          </div>
          <NewsEventPeriod startAt={news.eventStartAt} endAt={news.eventEndAt} />
        </section>
      )}

      <div className="mt-7 text-base leading-[1.85] break-words whitespace-pre-line text-gray-600">
        {news.body}
      </div>

      {news.cta && (
        <a
          href={news.cta.url}
          className="focus-visible:outline-primary-600 mt-7 inline-flex h-11 items-center justify-center rounded-lg bg-gray-900 px-5.5 text-sm font-semibold text-white hover:bg-gray-700 focus-visible:outline-2 focus-visible:outline-offset-2"
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
