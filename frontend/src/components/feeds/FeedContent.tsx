import Markdown from 'react-markdown';
import type { Feed } from '@/apis/feed';
import { Image } from '@/components/Image';
import { LinkPreview } from '@/components/feeds/LinkPreview';

const FIRST_URL_PATTERN = /https?:\/\/[^\s<>()]+/;

export function findFirstUrl(content: string) {
  return content.match(FIRST_URL_PATTERN)?.[0].replace(/[.,!?;:]+$/, '');
}

/**
 * 피드 본문. 카드와 상세가 같이 쓴다.
 *
 * 제목은 본문 위에 굵게 얹는다. 카드에서는 `h3`, 상세에서는 `h2`가 맞아 `titleAs`로 받는다
 * (상세는 위에 다른 제목이 없어 이 제목이 문서의 두 번째 단계다).
 */
export function FeedContent({
  feed,
  titleAs: Title = 'h3',
}: {
  feed: Feed;
  titleAs?: 'h2' | 'h3';
}) {
  const media = [...feed.media].sort((a, b) => a.displayOrder - b.displayOrder);
  const firstUrl = findFirstUrl(feed.content);

  return (
    <>
      <Title className="mt-4 text-base leading-snug font-bold break-words text-gray-900 md:text-lg">
        {feed.title}
      </Title>
      <div className="mt-2 space-y-3 text-base leading-7 break-words text-gray-800">
        <Markdown
          skipHtml
          components={{
            a: ({ href, children }) => (
              <a
                href={href}
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary-600 underline"
              >
                {children}
              </a>
            ),
            img: () => null,
            pre: ({ children }) => (
              <pre className="overflow-x-auto rounded-lg bg-gray-50 p-3">{children}</pre>
            ),
            ul: ({ children }) => <ul className="list-inside list-disc">{children}</ul>,
            ol: ({ children }) => <ol className="list-inside list-decimal">{children}</ol>,
          }}
        >
          {feed.content}
        </Markdown>
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
