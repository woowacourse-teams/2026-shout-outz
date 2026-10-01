import type { ReactNode } from 'react';
import type { Feed } from '@/apis/feed';
import { Image } from '@/components/Image';
import { FeedMarkdown } from '@/components/feeds/FeedMarkdown';
import { LinkPreview } from '@/components/feeds/LinkPreview';

/** 상세의 제목, 작성자, 본문, 첨부를 읽는 순서대로 보여준다. */
export function FeedDetailBody({ feed, author }: { feed: Feed; author?: ReactNode }) {
  const media = [...feed.media].sort((a, b) => a.displayOrder - b.displayOrder);
  const linkPreview = feed.linkPreview;

  return (
    <>
      <p className="text-xs leading-5 font-medium text-gray-600">
        {feed.categories.map((category) => category.displayName).join(' · ') ||
          (feed.feedType === 'QUESTION' ? '질문' : '이야기')}
      </p>
      <h1 className="mt-3 text-2xl leading-snug font-bold tracking-tight break-words text-gray-900 md:text-3xl">
        {feed.feedType === 'QUESTION' && <span className="text-primary-600 mr-2">Q.</span>}
        {feed.title}
      </h1>
      {author && <div className="mt-7">{author}</div>}
      <div className="mt-7 space-y-6 border-t border-gray-100 pt-8 text-sm leading-6 break-words text-gray-600">
        <FeedMarkdown content={feed.content} />
      </div>
      {linkPreview?.url && (
        <div className="mt-4">
          <LinkPreview {...linkPreview} url={linkPreview.url} />
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
