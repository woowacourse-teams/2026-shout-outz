import type { Feed } from '@/apis/feed';
import { Image } from '@/components/Image';
import { FeedMarkdown } from '@/components/feeds/FeedMarkdown';
import { LinkPreview } from '@/components/feeds/LinkPreview';
import { findFirstUrl } from '@/utils/feed';

/**
 * 피드 상세의 제목·본문·첨부. 전체 내용은 여기서만 보여준다.
 *
 * 제목은 `h2`다. 상세는 위에 다른 제목이 없어 이 제목이 문서의 두 번째 단계다.
 * 목록 카드는 본문을 잘라 보여주므로 `FeedCard`가 따로 그린다.
 */
export function FeedDetailBody({ feed }: { feed: Feed }) {
  const media = [...feed.media].sort((a, b) => a.displayOrder - b.displayOrder);
  const firstUrl = findFirstUrl(feed.content);

  return (
    <>
      <h2 className="mt-4 text-base leading-snug font-bold break-words text-gray-900 md:text-lg">
        {feed.title}
      </h2>
      <div className="mt-2 space-y-3 text-base leading-7 break-words text-gray-800">
        <FeedMarkdown content={feed.content} />
      </div>
      {firstUrl && (
        <div className="mt-4">
          <LinkPreview url={firstUrl} />
        </div>
      )}
      {media.length > 0 && (
        <div className="mt-4 space-y-3">
          {/* 조회 응답의 미디어는 공개 URL만 내려온다. mediaId로 따로 조회하던 경로는 없앴다. */}
          {media.map(({ url, displayOrder }) => (
            <Image
              key={`${displayOrder}-${url}`}
              src={url}
              alt="피드 첨부 이미지"
              loading="lazy"
              className="max-h-96 w-full rounded-xl bg-gray-50 object-contain"
              fallback={<p className="text-sm text-gray-500">이미지를 불러오지 못했습니다.</p>}
            />
          ))}
        </div>
      )}
    </>
  );
}
