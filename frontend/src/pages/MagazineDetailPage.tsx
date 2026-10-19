import { useSuspenseQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { IconArrowLeft, IconEye, IconHeart, IconMessageCircle } from '@tabler/icons-react';
import { magazineQuery, type Magazine } from '@/apis/magazine';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Avatar } from '@/components/Avatar';
import { Footer } from '@/components/Footer';
import { MarkdownContent } from '@/components/MarkdownContent';
import { UserHandle } from '@/components/users/UserHandle';
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
        <div className="flex min-w-0 items-center gap-2">
          <Avatar
            src={magazine.author.avatarUrl}
            name={magazine.author.displayName ?? undefined}
            alt=""
          />
          <div className="min-w-0">
            <p className="text-xs leading-5 text-gray-500">에디터</p>
            <p className="flex min-w-0 items-baseline gap-1.5 text-xs leading-5 font-semibold text-gray-900">
              <span className="truncate">{magazine.author.displayName ?? '탈퇴한 사용자'}</span>
              <UserHandle handle={magazine.author.handle} />
            </p>
          </div>
        </div>
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
      {magazine.featuredUser && <FeaturedUserIntro featuredUser={magazine.featuredUser} />}
      <div className="mt-7 border-t border-gray-100 pt-8">
        <MarkdownContent>{magazine.contentMd}</MarkdownContent>
      </div>
    </article>
  );
}

/** 매거진에서 소개하는 인터뷰이. 소개문은 회원 프로필과 별개로 매거진마다 따로 저장된다. */
function FeaturedUserIntro({
  featuredUser,
}: {
  featuredUser: NonNullable<Magazine['featuredUser']>;
}) {
  const profile = (
    <>
      <Avatar size="lg" src={featuredUser.avatarUrl} name={featuredUser.displayName} alt="" />
      <div className="min-w-0">
        <p className="text-primary-600 text-xs leading-5 font-medium">인터뷰이</p>
        <p className="group-hover:text-primary-600 flex min-w-0 flex-wrap items-baseline gap-x-1.5 text-base leading-6 font-bold text-gray-900">
          {featuredUser.displayName}
          <UserHandle handle={featuredUser.handle} />
        </p>
      </div>
    </>
  );

  return (
    <section aria-label="인터뷰이 소개" className="mt-7 rounded-lg bg-gray-50 p-5">
      {featuredUser.handle ? (
        <Link
          to="/users/$handle"
          params={{ handle: featuredUser.handle }}
          aria-label={`${featuredUser.displayName} 프로필 보기`}
          className="group focus-visible:outline-primary-600 flex w-fit min-w-0 items-center gap-3 rounded-sm focus-visible:outline-2"
        >
          {profile}
        </Link>
      ) : (
        <div className="flex min-w-0 items-center gap-3">{profile}</div>
      )}
      {featuredUser.introduction && (
        <p className="mt-3 text-sm leading-6 break-words whitespace-pre-line text-gray-600">
          {featuredUser.introduction}
        </p>
      )}
    </section>
  );
}
