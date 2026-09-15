import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Markdown from 'react-markdown';
import type { Feed } from '@/apis/feed';
import { kyInstance } from '@/utils/http';
import { Button } from '@/components/Button';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { FeedAuthor } from '@/components/feeds/FeedAuthor';
import { Comments } from '@/components/feed-comments/Comments';

function relativeTime(value: string) {
  const hours = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 3_600_000));
  return hours < 1 ? '방금 전' : hours < 24 ? `${hours}시간 전` : `${Math.floor(hours / 24)}일 전`;
}

function FeedImage({ id }: { id: number }) {
  const [failed, setFailed] = useState(false);
  const query = useQuery({
    queryKey: ['feed-media', id],
    queryFn: () => kyInstance.get(`/api/v1/media/${id}`).json<{ downloadUrl: string }>(),
    retry: false,
  });
  if (query.isError || failed)
    return <p className="text-sm text-gray-500">이미지를 불러오지 못했습니다.</p>;
  return query.data ? (
    <img
      src={query.data.downloadUrl}
      alt="피드 첨부 이미지"
      loading="lazy"
      onError={() => setFailed(true)}
      className="max-h-96 w-full rounded-xl bg-gray-50 object-contain"
    />
  ) : (
    <div className="h-40 animate-pulse rounded-xl bg-gray-50" aria-label="이미지 불러오는 중" />
  );
}

export function FeedCard({ feed }: { feed: Feed }) {
  const [open, setOpen] = useState(false);
  return (
    <article className="min-w-0 border-b border-gray-100 py-6 first:pt-4 md:py-7">
      <header className="flex items-start gap-3">
        <div className="min-w-0 flex-1">
          <FeedAuthor
            feed={feed}
            detail={
              <time className="text-sm text-gray-400" dateTime={feed.createdAt}>
                {relativeTime(feed.createdAt)}
              </time>
            }
          />
        </div>
        <Button variant="ghost" size="sm" aria-label="피드 메뉴">
          ···
        </Button>
      </header>
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
      {feed.media.length > 0 && (
        <div className="mt-4 space-y-3">
          {[...feed.media]
            .sort((a, b) => a.displayOrder - b.displayOrder)
            .map((media) => (
              <FeedImage key={media.mediaId} id={media.mediaId} />
            ))}
        </div>
      )}
      <div className="mt-5 flex items-center gap-2 text-sm text-gray-500">
        <Button variant="ghost" size="sm" onClick={() => {}}>
          좋아요
        </Button>
        <Button
          variant="ghost"
          size="sm"
          aria-expanded={open}
          aria-controls={`comments-${feed.postId}`}
          onClick={() => setOpen(!open)}
        >
          댓글
        </Button>
        <span className="ml-auto">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => void navigator.clipboard?.writeText(window.location.href)}
          >
            공유
          </Button>
        </span>
      </div>
      {open && (
        <section
          id={`comments-${feed.postId}`}
          aria-label={`${feed.author.displayName} 피드 댓글`}
          className="mt-5 border-t border-gray-100 pt-5"
        >
          <AsyncBoundary>
            <Comments postId={feed.postId} />
          </AsyncBoundary>
        </section>
      )}
    </article>
  );
}
