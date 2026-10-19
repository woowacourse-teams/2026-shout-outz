import { useSuspenseQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { IconArrowLeft, IconEye, IconHeart, IconMessageCircle } from '@tabler/icons-react';
import { magazineQuery } from '@/apis/magazine';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Avatar } from '@/components/Avatar';
import { Footer } from '@/components/Footer';
import { MarkdownContent } from '@/components/MarkdownContent';
import { formatDotDate } from '@/utils/date';

export function MagazineDetailPage({ magazineId }: { magazineId: number }) {
  return (
    <div className="bg-background flex min-h-dvh flex-col">
      <AppGnb />
      <main className="mx-auto w-full max-w-4xl flex-1 px-5 pt-7 pb-16 md:px-12 md:pt-8 md:pb-20">
        <section className="min-w-0" aria-label="매거진 상세">
          <AsyncBoundary key={magazineId}>
            <MagazineDetailContent magazineId={magazineId} />
          </AsyncBoundary>
        </section>
      </main>
      <Footer />
    </div>
  );
}

function MagazineDetailContent({ magazineId }: { magazineId: number }) {
  const { data: magazine } = useSuspenseQuery(magazineQuery(magazineId));
  const publishedAt = magazine.publishedAt ?? magazine.createdAt;

  return (
    <article className="min-w-0">
      <title>{`${magazine.title} | shout-outz`}</title>
      <Link
        to="/magazines"
        className="mb-7 inline-flex items-center gap-2 text-xs text-gray-500 hover:text-gray-900"
      >
        <IconArrowLeft className="size-4" aria-hidden="true" />
        목록으로
      </Link>
      <p className="text-xs leading-5 font-medium text-gray-600">매거진</p>
      <h1 className="mt-3 text-2xl leading-snug font-bold tracking-tight break-words text-gray-900 md:text-3xl">
        {magazine.title}
      </h1>
      {magazine.summary && (
        <p className="mt-3 text-sm leading-6 break-words text-gray-600">{magazine.summary}</p>
      )}
      <div className="mt-7 flex flex-wrap items-center justify-between gap-3">
        {magazine.featuredUser && (
          <div className="flex min-w-0 items-center gap-2">
            <Avatar
              src={magazine.featuredUser.avatarUrl}
              name={magazine.featuredUser.displayName}
              alt=""
            />
            <div className="min-w-0">
              <p className="text-xs leading-5 font-semibold text-gray-900">
                {magazine.featuredUser.displayName}
              </p>
              {magazine.featuredUser.introduction && (
                <p className="truncate text-xs leading-4 text-gray-500">
                  {magazine.featuredUser.introduction}
                </p>
              )}
            </div>
          </div>
        )}
        <div className="ml-auto flex items-center gap-3 text-xs text-gray-500">
          <time dateTime={publishedAt}>{formatDotDate(publishedAt)}</time>
          <span
            className="inline-flex items-center gap-1"
            aria-label={`조회 ${magazine.viewCount}회`}
          >
            <IconEye className="size-4" aria-hidden="true" />
            {magazine.viewCount}
          </span>
          <span
            className="inline-flex items-center gap-1"
            aria-label={`좋아요 ${magazine.likeCount}개`}
          >
            <IconHeart className="size-4" aria-hidden="true" />
            {magazine.likeCount}
          </span>
          <span
            className="inline-flex items-center gap-1"
            aria-label={`댓글 ${magazine.commentCount}개`}
          >
            <IconMessageCircle className="size-4" aria-hidden="true" />
            {magazine.commentCount}
          </span>
        </div>
      </div>
      <div className="mt-7 border-t border-gray-100 pt-8">
        <MarkdownContent>{magazine.contentMd}</MarkdownContent>
      </div>
    </article>
  );
}
