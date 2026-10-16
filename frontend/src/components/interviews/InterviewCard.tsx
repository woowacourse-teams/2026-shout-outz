import { Link } from '@tanstack/react-router';
import { IconEye, IconHeart, IconHeartFilled, IconMessageCircle } from '@tabler/icons-react';
import type { InterviewListItem } from '@/apis/interview';
import { Avatar } from '@/components/Avatar';
import { Image } from '@/components/Image';
import { formatDotDate } from '@/utils/date';

export function InterviewCard({ interview }: { interview: InterviewListItem }) {
  const publishedAt = interview.publishedAt ?? interview.createdAt;

  return (
    <article className="group bg-background relative flex min-w-0 flex-col overflow-hidden rounded-lg border border-gray-200 transition-colors hover:border-gray-300">
      <Link
        to="/interviews/$interviewId"
        params={{ interviewId: String(interview.id) }}
        aria-label={interview.title}
        className="focus-visible:outline-primary-600 absolute inset-0 z-10 rounded-lg focus-visible:outline-2 focus-visible:outline-offset-2"
      />
      <Image
        src={interview.thumbnailUrl ?? undefined}
        alt=""
        loading="lazy"
        className="aspect-video w-full bg-gray-50"
        fallback={<div className="aspect-video w-full bg-gray-50" />}
      />
      <div className="flex flex-1 flex-col p-5">
        <div className="flex-1">
          <h3 className="group-hover:text-primary-600 text-base leading-6 font-semibold tracking-tight break-words text-gray-900">
            {interview.title}
          </h3>
          {interview.summary && (
            <p className="mt-2 line-clamp-2 text-sm leading-6 break-words text-gray-600">
              {interview.summary}
            </p>
          )}
        </div>
        <div className="mt-5 flex items-center justify-between gap-3 border-t border-gray-100 pt-4">
          {interview.interviewee ? (
            <div className="flex min-w-0 items-center gap-2">
              <Avatar
                size="sm"
                src={interview.interviewee.avatarUrl}
                name={interview.interviewee.displayName}
                alt=""
              />
              <span className="truncate text-xs font-semibold text-gray-900">
                {interview.interviewee.displayName}
              </span>
            </div>
          ) : (
            <span />
          )}
          <time dateTime={publishedAt} className="shrink-0 text-xs text-gray-500">
            {formatDotDate(publishedAt)}
          </time>
        </div>
        <div className="mt-3 flex items-center gap-3 text-xs text-gray-500" aria-label="반응 수">
          <span
            className="inline-flex items-center gap-1"
            aria-label={`조회 ${interview.viewCount}회`}
          >
            <IconEye className="size-4" aria-hidden="true" />
            {interview.viewCount}
          </span>
          <span
            className={`inline-flex items-center gap-1 ${interview.likedByMe ? 'text-primary-600' : ''}`}
            aria-label={`좋아요 ${interview.likeCount}개${interview.likedByMe ? ', 내가 누름' : ''}`}
          >
            {interview.likedByMe ? (
              <IconHeartFilled className="text-primary-600 size-4" aria-hidden="true" />
            ) : (
              <IconHeart className="size-4" aria-hidden="true" />
            )}
            {interview.likeCount}
          </span>
          <span
            className="inline-flex items-center gap-1"
            aria-label={`댓글 ${interview.commentCount}개`}
          >
            <IconMessageCircle className="size-4" aria-hidden="true" />
            {interview.commentCount}
          </span>
        </div>
      </div>
    </article>
  );
}
