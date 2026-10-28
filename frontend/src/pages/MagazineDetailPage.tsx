import { Title } from '@/components/Title';
import { useSuspenseQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { IconArrowLeft, IconHeart } from '@tabler/icons-react';
import { magazineQuery, type Magazine } from '@/apis/magazine';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Avatar } from '@/components/Avatar';
import { Badge } from '@/components/Badge';
import { ClientOnly } from '@/components/ClientOnly';
import { Comments } from '@/components/feed-comments/Comments';
import { FeedLikeButton } from '@/components/feeds/FeedLikeButton';
import { Footer } from '@/components/Footer';
import { MarkdownContent } from '@/components/MarkdownContent';
import { UserHandle } from '@/components/users/UserHandle';
import { formatDotDate } from '@/utils/date';

export function MagazineDetailPage({ magazineId }: { magazineId: number }) {
  return (
    <div className="bg-background flex min-h-dvh flex-col">
      <AppGnb />
      <main className="mx-auto w-full max-w-180 flex-1 px-6 pt-10 pb-28">
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
      <Title>{`${magazine.title} | shout-outz`}</Title>
      <Link
        to="/magazines"
        className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-900"
      >
        <IconArrowLeft className="size-4" aria-hidden="true" />
        목록으로
      </Link>
      {/* ASSUMPTION: 매거진 분류 값이 없어, 인터뷰이가 있는 글에 "인터뷰" 배지를 단다. */}
      {magazine.featuredUser && (
        <p className="mt-7">
          <Badge tone="primary" className="px-2">
            인터뷰
          </Badge>
        </p>
      )}
      <h1
        className={`text-2xl leading-tight font-extrabold tracking-tight break-keep text-gray-900 md:text-3xl ${magazine.featuredUser ? 'mt-3' : 'mt-7'}`}
      >
        {magazine.title}
      </h1>
      {magazine.summary && (
        <p className="mt-3 text-base leading-relaxed break-words text-gray-600">
          {magazine.summary}
        </p>
      )}
      <div className="mt-5 flex flex-wrap items-center justify-between gap-x-4 gap-y-2 text-xs">
        <div className="flex min-w-0 items-center gap-2.5">
          <Avatar
            src={magazine.author.avatarUrl}
            name={magazine.author.displayName ?? undefined}
            alt=""
          />
          <p className="flex min-w-0 flex-wrap items-baseline gap-x-1">
            <span className="text-gray-500">에디터</span>
            <b className="font-bold text-gray-900">
              {magazine.author.displayName ?? '탈퇴한 사용자'}
            </b>
            <UserHandle handle={magazine.author.handle} />
          </p>
        </div>
        <p className="text-gray-500">
          <time dateTime={publishedAt}>{formatDotDate(publishedAt)}</time>
          {' · '}
          <span aria-label={`조회 ${magazine.viewCount}회`}>조회 {magazine.viewCount}</span>
          {' · '}
          <span aria-label={`좋아요 ${magazine.likeCount}개`}>
            <IconHeart className="inline size-3.5 align-[-2px]" aria-hidden="true" />{' '}
            {magazine.likeCount}
          </span>
          {' · '}
          <span aria-label={`댓글 ${magazine.commentCount}개`}>댓글 {magazine.commentCount}</span>
        </p>
      </div>
      {magazine.featuredUser && <FeaturedUserIntro featuredUser={magazine.featuredUser} />}
      <div className="mt-8">
        <MarkdownContent preserveBlankLines>{magazine.contentMd}</MarkdownContent>
      </div>
      <ClientOnly>
        <div className="mt-8 flex justify-center">
          <FeedLikeButton
            target="magazines"
            feedId={magazine.id}
            likeCount={magazine.likeCount}
            likedByMe={magazine.likedByMe}
            size="lg"
          />
        </div>
        <section id="magazine-comments" aria-labelledby="magazine-comments-title" className="mt-14">
          <h2 id="magazine-comments-title" className="mb-3.5 text-xl font-bold text-gray-900">
            댓글 <span className="text-primary-600">{magazine.commentCount}</span>
          </h2>
          <Comments target="magazines" feedId={magazine.id} />
        </section>
      </ClientOnly>
    </article>
  );
}

/** 매거진에서 소개하는 인터뷰이. 소개문은 회원 프로필과 별개로 매거진마다 따로 저장된다. */
function FeaturedUserIntro({
  featuredUser,
}: {
  featuredUser: NonNullable<Magazine['featuredUser']>;
}) {
  const name = (
    <span className="flex min-w-0 flex-wrap items-baseline gap-x-1">
      <span className="text-sm font-bold text-gray-900 group-hover:underline">
        {featuredUser.displayName}
      </span>
      <UserHandle handle={featuredUser.handle} />
    </span>
  );

  return (
    <section
      aria-label="인터뷰이 소개"
      className="mt-5 flex items-center gap-3.5 rounded-xl border border-gray-100 bg-gray-50 px-5 py-4.5"
    >
      <Avatar
        size="lg"
        src={featuredUser.avatarUrl}
        name={featuredUser.displayName}
        alt=""
        className="size-11 shrink-0"
      />
      <div className="min-w-0">
        <p className="text-primary-600 text-xs font-bold">인터뷰이</p>
        {featuredUser.handle ? (
          <Link
            to="/users/$handle"
            params={{ handle: featuredUser.handle }}
            aria-label={`${featuredUser.displayName} 프로필 보기`}
            className="group focus-visible:outline-primary-600 block w-fit rounded-sm focus-visible:outline-2"
          >
            {name}
          </Link>
        ) : (
          name
        )}
        {featuredUser.introduction && (
          <p className="mt-0.5 text-xs leading-relaxed break-words whitespace-pre-line text-gray-600">
            {featuredUser.introduction}
          </p>
        )}
      </div>
    </section>
  );
}
