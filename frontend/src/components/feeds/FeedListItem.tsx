import { useId } from 'react';
import { Link } from '@tanstack/react-router';
import { IconHeart, IconHeartFilled, IconMessageCircle } from '@tabler/icons-react';
import type { Feed } from '@/apis/feed';
import { Image } from '@/components/Image';
import { FeedAuthor } from '@/components/feeds/FeedAuthor';
import { toPlainText } from '@/utils/markdown';
import { formatRelativeTime } from '@/utils/date';
import { formatFeedCardLabel, formatFeedCardPreview, formatFeedCategory } from '@/utils/feed';
import { analytics, type FeedSurface } from '@/utils/analytics';
import { cn } from '@/utils/cn';

/**
 * 커뮤니티 목록의 한 줄. 카드 테두리 없이 위쪽 구분선으로 나눈다(구분선은 목록이 그린다).
 *
 * 스크린리더에서는 카드 하나가 제목 링크 하나로 읽힌다. 링크 이름은 제목 뒤에 카드의 정보를 이어 붙이고,
 * 본문 미리보기는 설명으로 연결한다. 화면에 보이는 나머지 요소는 같은 내용을 다시 읽지 않도록 숨긴다.
 * 링크는 제목 안에 두되 가상 요소로 카드 전체를 덮어, 카드 어디를 눌러도 상세로 이동한다.
 */
export function FeedListItem({ feed, surface }: { feed: Feed; surface: FeedSurface }) {
  const previewId = useId();
  const isQuestion = feed.feedType === 'QUESTION';
  const commentCount = feed.commentCount ?? 0;
  const likeCount = feed.likeCount ?? 0;
  const [firstMedia, ...restMedia] = [...feed.media].sort(
    (a, b) => a.displayOrder - b.displayOrder,
  );
  const category = formatFeedCategory(feed);
  const preview = toPlainText(feed.content);
  const spokenPreview = formatFeedCardPreview(feed);

  return (
    <article className="relative flex min-w-0 flex-col gap-2.5">
      <h3 className="order-2 text-lg font-bold tracking-tight break-words text-gray-900">
        <Link
          to="/community/$feedId"
          params={{ feedId: String(feed.feedId) }}
          onClick={() =>
            analytics.track({ name: 'feed_detail_opened', feedId: feed.feedId, from: surface })
          }
          // 화면의 제목으로 시작해, 음성 명령으로 제목을 불러도 이 링크를 찾을 수 있다.
          aria-label={formatFeedCardLabel(feed)}
          aria-describedby={spokenPreview ? previewId : undefined}
          className="focus-visible:after:outline-primary-600 outline-none after:absolute after:inset-0 after:z-10 after:rounded-sm focus-visible:after:outline-2 focus-visible:after:outline-offset-4"
        >
          {isQuestion && (
            <span aria-hidden="true" className="text-primary-600 mr-2">
              Q.
            </span>
          )}
          {feed.title}
        </Link>
      </h3>
      {/* 미리보기는 화면처럼 앞부분만 설명으로 읽는다. 화면에는 보이지 않는다. */}
      {spokenPreview && (
        <span id={previewId} hidden>
          {spokenPreview}
        </span>
      )}
      <p
        aria-hidden="true"
        className="order-1 flex flex-wrap items-center gap-2 text-xs text-gray-400"
      >
        {isQuestion && (
          <span
            className={cn(
              'rounded-sm px-1.5 py-px font-bold',
              commentCount === 0
                ? 'bg-yellow-50 text-yellow-700'
                : 'bg-primary-50 text-primary-600',
            )}
          >
            {commentCount === 0 ? '답변 대기' : `답변 ${commentCount}`}
          </span>
        )}
        <span className="font-semibold text-gray-600">{category}</span>
        <time dateTime={feed.createdAt}>{formatRelativeTime(feed.createdAt)}</time>
      </p>
      {preview && (
        <p
          aria-hidden="true"
          className="order-3 line-clamp-2 text-sm leading-6 break-words text-gray-600"
        >
          {preview}
        </p>
      )}
      {firstMedia && (
        <div aria-hidden="true" className="pointer-events-none relative order-4 w-36">
          <Image
            src={firstMedia.url}
            alt="피드 첨부 이미지"
            loading="lazy"
            className="h-24 w-36 rounded-lg bg-gray-50 object-contain"
            fallback={<p className="text-sm text-gray-500">이미지를 불러오지 못했습니다.</p>}
          />
          {restMedia.length > 0 && (
            <span className="absolute right-3 bottom-3 rounded-full bg-gray-900/70 px-2.5 py-1 text-xs font-bold text-white">
              +{restMedia.length}
            </span>
          )}
        </div>
      )}
      <div aria-hidden="true" className="order-5 mt-1 flex items-center justify-between gap-3">
        <div className="min-w-0">
          <FeedAuthor
            author={feed.author}
            isAnonymous={feed.isAnonymous}
            avatarSize="sm"
            profileLink={false}
            badgeFocusable={false}
          />
        </div>
        <div className="flex shrink-0 items-center gap-3.5 text-xs text-gray-500">
          <span
            className={cn('inline-flex items-center gap-1', feed.likedByMe && 'text-primary-600')}
          >
            {feed.likedByMe ? (
              <IconHeartFilled className="size-4" aria-hidden="true" />
            ) : (
              <IconHeart className="size-4" aria-hidden="true" />
            )}
            {likeCount}
          </span>
          <span className="inline-flex items-center gap-1">
            <IconMessageCircle className="size-4" aria-hidden="true" />
            {commentCount}
          </span>
        </div>
      </div>
    </article>
  );
}
