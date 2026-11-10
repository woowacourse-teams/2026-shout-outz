import type { ReactNode } from 'react';
import type { Feed } from '@/apis/feed';
import { Image } from '@/components/Image';
import { FeedMarkdown } from '@/components/feeds/FeedMarkdown';
import { LinkPreview } from '@/components/feeds/LinkPreview';
import { analytics } from '@/utils/analytics';

/** 상세의 제목, 작성자, 본문, 첨부를 읽는 순서대로 보여준다. */
export function FeedDetailBody({ feed, author }: { feed: Feed; author?: ReactNode }) {
  const media = [...feed.media].sort((a, b) => a.displayOrder - b.displayOrder);
  const linkPreview = feed.linkPreview;

  return (
    <>
      <p className="text-xs font-semibold text-gray-500">
        {feed.categories.map((category) => category.displayName).join(' · ') ||
          (feed.feedType === 'QUESTION' ? '질문' : '이야기')}
      </p>
      <h1 className="mt-2.5 text-2xl leading-tight font-extrabold tracking-tight break-words text-gray-900 md:text-3xl">
        {feed.feedType === 'QUESTION' && <span className="text-primary-600 mr-2">Q.</span>}
        {feed.title}
      </h1>
      {author && <div className="mt-5 border-b border-gray-100 pb-5">{author}</div>}
      {/* 글자 크기는 노션 기준: 본문 16px, 제목 30/24/20px, 코드 85% */}
      <div className="mt-7 space-y-6 text-base leading-7 break-words text-gray-600 [&_code]:text-[85%] [&_h1]:text-[1.875rem] [&_h1]:leading-[1.3] [&_h1]:font-bold [&_h1]:text-gray-900 [&_h2]:text-2xl [&_h2]:leading-[1.3] [&_h2]:font-bold [&_h2]:text-gray-900 [&_h3]:text-xl [&_h3]:leading-[1.3] [&_h3]:font-bold [&_h3]:text-gray-900">
        <FeedMarkdown content={feed.content} />
      </div>
      {linkPreview?.url && (
        // PROTOTYPE-DIFF: 링크 미리보기는 댓글에서도 써서 기존 모양을 둔다. 프로토타입은 회색 테두리 카드에 왼쪽 아이콘 칸이 있다.
        <div className="mt-5">
          <LinkPreview
            {...linkPreview}
            url={linkPreview.url}
            onClick={() => analytics.track({ name: 'link_preview_clicked', feedId: feed.feedId })}
          />
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
