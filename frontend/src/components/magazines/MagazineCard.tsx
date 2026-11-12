import { Link } from '@tanstack/react-router';
import { IconEye, IconHeart, IconHeartFilled, IconMessageCircle } from '@tabler/icons-react';
import type { MagazineListItem } from '@/apis/magazine';
import { Avatar } from '@/components/Avatar';
import { Badge } from '@/components/Badge';
import { Image } from '@/components/Image';
import { analytics } from '@/utils/analytics';
import { cn } from '@/utils/cn';
import { formatDotDate } from '@/utils/date';

interface MagazineCardProps {
  magazine: MagazineListItem;
  /** 목록 맨 위의 큰 카드. 데스크톱에서 사진과 글을 가로로 놓고 소개 글까지 보여 준다. */
  featured?: boolean;
}

export function MagazineCard({ magazine, featured = false }: MagazineCardProps) {
  const publishedAt = magazine.publishedAt ?? magazine.createdAt;

  return (
    <article className="min-w-0">
      <Link
        to="/magazines/$magazineId"
        params={{ magazineId: String(magazine.id) }}
        onClick={() =>
          analytics.track({ name: 'magazine_card_clicked', magazineId: magazine.id, featured })
        }
        className={cn(
          'group focus-visible:outline-primary-600 grid rounded-xl focus-visible:outline-2 focus-visible:outline-offset-4',
          featured ? 'gap-4 md:grid-cols-[340px_minmax(0,1fr)] md:gap-8' : 'gap-3',
        )}
      >
        <Image
          src={magazine.thumbnailUrl ?? undefined}
          alt=""
          loading="lazy"
          className="aspect-[16/10] w-full rounded-xl bg-gray-100"
          fallback={<div className="aspect-[16/10] w-full rounded-xl bg-gray-100" />}
        />
        <div className={cn('flex min-w-0 flex-col', featured ? 'gap-3.5 md:py-4' : 'gap-3')}>
          <p className="flex items-center gap-2 text-xs text-gray-400">
            {/* ASSUMPTION: 매거진 분류 값이 없어, 인터뷰이가 있는 글에 "인터뷰" 배지를 단다. */}
            {magazine.featuredUser && <Badge tone="primary">인터뷰</Badge>}
            <time dateTime={publishedAt}>{formatDotDate(publishedAt)}</time>
          </p>
          {featured ? (
            <h2 className="text-2xl leading-snug font-extrabold tracking-tight break-keep text-gray-900 group-hover:underline">
              {magazine.title}
            </h2>
          ) : (
            <h3 className="text-lg leading-snug font-bold tracking-tight break-keep text-gray-900 group-hover:underline">
              {magazine.title}
            </h3>
          )}
          {featured && magazine.summary && (
            <p className="text-sm leading-relaxed break-words text-gray-700 md:text-base">
              {magazine.summary}
            </p>
          )}
          <div className={cn('flex items-center justify-between gap-3', featured && 'mt-1.5')}>
            <span className="flex min-w-0 items-center gap-2 text-xs text-gray-500">
              <Avatar
                size="xs"
                src={magazine.author.avatarUrl}
                name={magazine.author.displayName ?? undefined}
                alt=""
                className="size-6 shrink-0"
              />
              <span className="truncate">
                글{' '}
                <b className="font-bold text-gray-600">
                  {magazine.author.displayName ?? '탈퇴한 사용자'}
                </b>
              </span>
            </span>
            <span
              className={cn(
                'flex shrink-0 items-center text-xs text-gray-500',
                featured ? 'gap-3.5' : 'gap-3',
              )}
            >
              <span
                className="inline-flex items-center gap-1"
                aria-label={`조회 ${magazine.viewCount}회`}
              >
                <IconEye className="size-3.5" aria-hidden="true" />
                {magazine.viewCount}
              </span>
              <span
                className={cn(
                  'inline-flex items-center gap-1',
                  magazine.likedByMe && 'text-primary-600',
                )}
                aria-label={`좋아요 ${magazine.likeCount}개${magazine.likedByMe ? ', 내가 누름' : ''}`}
              >
                {magazine.likedByMe ? (
                  <IconHeartFilled className="size-3.5" aria-hidden="true" />
                ) : (
                  <IconHeart className="size-3.5" aria-hidden="true" />
                )}
                {magazine.likeCount}
              </span>
              <span
                className="inline-flex items-center gap-1"
                aria-label={`댓글 ${magazine.commentCount}개`}
              >
                <IconMessageCircle className="size-3.5" aria-hidden="true" />
                {magazine.commentCount}
              </span>
            </span>
          </div>
        </div>
      </Link>
    </article>
  );
}
