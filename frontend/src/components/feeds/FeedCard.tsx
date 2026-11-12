import { useId } from 'react';
import { Link } from '@tanstack/react-router';
import { IconHeart, IconHeartFilled, IconMessageCircle, IconSparkles } from '@tabler/icons-react';
import type { Feed } from '@/apis/feed';
import { Image } from '@/components/Image';
import { FeedAuthor } from '@/components/feeds/FeedAuthor';
import { toPlainText } from '@/utils/markdown';
import { formatRelativeTime } from '@/utils/date';
import { formatFeedCardLabel, formatFeedCardPreview, formatFeedCategory } from '@/utils/feed';
import { analytics, type FeedSurface } from '@/utils/analytics';

/**
 * 테두리가 있는 피드 카드. 프로필과 홈에서 쓴다.
 *
 * 커뮤니티 목록(`FeedListItem`)과 같은 방식으로, 스크린리더에서는 카드 하나가 제목 링크 하나로 읽힌다.
 * 링크 이름에 카드 정보를 모으고 본문 미리보기는 설명으로 연결하며, 화면의 나머지 요소는 숨긴다.
 * 링크는 제목 안에 두되 가상 요소로 카드 전체를 덮어, 카드 어디를 눌러도 상세로 이동한다.
 */
export function FeedCard({ feed, surface }: { feed: Feed; surface: FeedSurface }) {
  const previewId = useId();
  const isQuestion = feed.feedType === 'QUESTION';
  const [firstMedia, ...restMedia] = [...feed.media].sort(
    (a, b) => a.displayOrder - b.displayOrder,
  );
  const preview = toPlainText(feed.content);
  const spokenPreview = formatFeedCardPreview(feed);

  return (
    <article className="group bg-background relative flex min-w-0 flex-col rounded-lg border border-gray-200 p-5 transition-colors hover:border-gray-300 md:p-6">
      <h3 className="group-hover:text-primary-600 order-2 text-base leading-6 font-semibold tracking-tight break-words text-gray-900">
        <Link
          to="/community/$feedId"
          params={{ feedId: String(feed.feedId) }}
          onClick={() =>
            analytics.track({ name: 'feed_detail_opened', feedId: feed.feedId, from: surface })
          }
          aria-label={formatFeedCardLabel(feed)}
          aria-describedby={spokenPreview ? previewId : undefined}
          className="focus-visible:after:outline-primary-600 outline-none after:absolute after:inset-0 after:z-10 after:rounded-lg focus-visible:after:outline-2 focus-visible:after:outline-offset-2"
        >
          {isQuestion && (
            <span aria-hidden="true" className="text-primary-600 mr-1.5">
              Q.
            </span>
          )}
          {feed.title}
        </Link>
      </h3>
      {spokenPreview && (
        <span id={previewId} hidden>
          {spokenPreview}
        </span>
      )}
      {/* 아래 정보는 링크 이름으로 함께 읽으므로, 같은 내용을 다시 읽지 않게 숨긴다. */}
      <div aria-hidden="true" className="order-1 mb-4 flex items-start justify-between gap-3">
        <div className="text-primary-600 flex flex-col items-start gap-3 text-xs leading-4 font-medium">
          {isQuestion && (feed.commentCount ?? 0) === 0 && (
            <span className="flex items-center gap-1">
              <IconSparkles className="size-3.5 text-yellow-500" aria-hidden="true" />
              답변을 기다리고 있어요
            </span>
          )}
          <span>{formatFeedCategory(feed)}</span>
        </div>
        <time dateTime={feed.createdAt} className="shrink-0 text-xs leading-4 text-gray-500">
          {formatRelativeTime(feed.createdAt)}
        </time>
      </div>
      {preview && (
        <p
          aria-hidden="true"
          className="order-3 mt-2 line-clamp-2 text-sm leading-6 break-words text-gray-600"
        >
          {preview}
        </p>
      )}
      {firstMedia && (
        <div aria-hidden="true" className="pointer-events-none relative order-4 mt-4 w-36">
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
      <div
        aria-hidden="true"
        className="order-5 mt-5 flex items-center justify-between gap-3 border-t border-gray-100 pt-4"
      >
        <div className="min-w-0">
          <FeedAuthor
            author={feed.author}
            isAnonymous={feed.isAnonymous}
            avatarSize="sm"
            profileLink={false}
            badgeFocusable={false}
          />
        </div>
        <div className="flex shrink-0 items-center gap-3 text-xs text-gray-500">
          <span
            className={`inline-flex items-center gap-1 ${feed.likedByMe ? 'text-primary-600' : ''}`}
          >
            {feed.likedByMe ? (
              <IconHeartFilled className="text-primary-600 size-4" aria-hidden="true" />
            ) : (
              <IconHeart className="size-4" aria-hidden="true" />
            )}
            {feed.likeCount ?? 0}
          </span>
          <span className="inline-flex items-center gap-1">
            <IconMessageCircle className="size-4" aria-hidden="true" />
            {feed.commentCount ?? 0}
          </span>
        </div>
      </div>
    </article>
  );
}
