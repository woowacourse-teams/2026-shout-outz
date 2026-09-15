import Markdown from 'react-markdown';
import type { Feed } from '@/apis/feed';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { FeedMedia } from '@/components/feeds/FeedMedia';

export function FeedContent({ feed }: { feed: Feed }) {
  const media = [...feed.media].sort((a, b) => a.displayOrder - b.displayOrder);

  return (
    <>
      <div className="mt-5 space-y-3 text-base leading-7 break-words text-gray-800">
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
      {media.length > 0 && (
        <div className="mt-4 space-y-3">
          {media.map(({ mediaId }) => (
            <AsyncBoundary key={mediaId}>
              <FeedMedia mediaId={mediaId} />
            </AsyncBoundary>
          ))}
        </div>
      )}
    </>
  );
}
