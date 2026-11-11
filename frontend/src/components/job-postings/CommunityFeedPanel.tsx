import { useQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { IconHeart, IconMessageCircle } from '@tabler/icons-react';

import { jobPostingCommunityFeedsQuery } from '@/apis/job-posting';
import { Avatar } from '@/components/Avatar';
import { formatDotDate } from '@/utils/date';

/** 채용공고 상세 오른쪽의 "연관 질문" */
export function CommunityFeedPanel({ jobPostingId }: { jobPostingId: number }) {
  const { data: feeds } = useQuery(jobPostingCommunityFeedsQuery(jobPostingId));

  // 질문 목록을 못 불러오거나 비어 있으면 패널을 숨긴다.
  if (!feeds || feeds.length === 0) return null;

  return (
    <aside aria-labelledby="job-community-title" className="flex flex-col">
      <h2 id="job-community-title" className="pb-3 text-sm font-bold tracking-tight">
        연관 질문
      </h2>
      <ul>
        {feeds.map((feed) => (
          <li key={feed.feedId} className="border-t border-gray-100">
            <Link
              to="/community/$feedId"
              params={{ feedId: String(feed.feedId) }}
              className="focus-visible:outline-primary-600 group flex flex-col gap-1.5 py-4 focus-visible:outline-2"
            >
              <span className="text-xs font-medium text-gray-500">
                <span className="text-primary-600">{feed.type}</span> · {feed.categoryName} ·{' '}
                <time dateTime={feed.createdAt}>{formatDotDate(feed.createdAt)}</time>
              </span>
              <h3 className="text-sm leading-snug font-bold tracking-tight break-keep text-gray-900 group-hover:underline">
                <span className="text-primary-600">Q. </span>
                {feed.title}
              </h3>
              <p className="line-clamp-3 text-xs leading-relaxed text-gray-600">{feed.content}</p>
              <div className="flex min-w-0 items-center gap-2 text-xs text-gray-500">
                <span className="flex min-w-0 flex-1 items-center gap-1.5">
                  <Avatar
                    src={feed.authorAvatarUrl}
                    name={feed.authorDisplay}
                    alt=""
                    size="xs"
                    className="shrink-0"
                  />
                  <span className="min-w-0 truncate font-semibold text-gray-600">
                    {feed.authorDisplay}
                  </span>
                  <span className="min-w-0 flex-1 truncate">{feed.authorAffiliation}</span>
                </span>
                <span className="flex shrink-0 gap-2.5">
                  <span
                    className="inline-flex items-center gap-0.5"
                    aria-label={`궁금해요 ${feed.likeCount}개`}
                  >
                    <IconHeart className="size-3.5" aria-hidden="true" />
                    {feed.likeCount}
                  </span>
                  <span
                    className="inline-flex items-center gap-0.5"
                    aria-label={`답변 ${feed.commentCount}개`}
                  >
                    <IconMessageCircle className="size-3.5" aria-hidden="true" />
                    {feed.commentCount}
                  </span>
                </span>
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </aside>
  );
}
