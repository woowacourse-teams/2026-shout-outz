import { useQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { useState } from 'react';

import { jobPostingCommunityFeedsQuery } from '@/apis/job-posting';

/** 채용공고 상세 오른쪽의 "커뮤니티에서 나온 고민" */
export function CommunityFeedPanel({ jobPostingId }: { jobPostingId: number }) {
  const { data: feeds } = useQuery(jobPostingCommunityFeedsQuery(jobPostingId));
  const [collapsed, setCollapsed] = useState(false);

  // ASSUMPTION: 고민 목록을 못 불러오거나 비어 있으면 패널을 숨긴다. 상세의 핵심은 왼쪽 자료라서다.
  if (!feeds || feeds.length === 0) return null;

  return (
    <aside aria-labelledby="job-community-title" className="flex flex-col">
      <div className="flex flex-col gap-1 pb-3">
        <div className="flex items-center justify-between gap-2">
          <h2 id="job-community-title" className="text-base font-bold tracking-tight">
            커뮤니티에서 나온 고민 <span className="font-medium text-gray-400">{feeds.length}</span>
          </h2>
          <button
            type="button"
            aria-expanded={!collapsed}
            aria-controls="job-community-list"
            aria-label={collapsed ? '커뮤니티 패널 펼치기' : '커뮤니티 패널 접기'}
            onClick={() => setCollapsed((value) => !value)}
            className="focus-visible:outline-primary-600 inline-flex size-6 cursor-pointer items-center justify-center rounded-sm text-sm text-gray-400 hover:text-gray-700 focus-visible:outline-2"
          >
            {collapsed ? '+' : '—'}
          </button>
        </div>
        {!collapsed && (
          <p className="text-sm leading-relaxed text-gray-500">
            이 공고를 준비하며 생길 고민을 크루들이 먼저 나눴어요.
          </p>
        )}
      </div>
      {!collapsed && (
        <ul id="job-community-list">
          {feeds.map((feed) => (
            <li key={feed.feedId} className="border-t border-gray-100">
              <Link
                to="/community/$feedId"
                params={{ feedId: String(feed.feedId) }}
                className="focus-visible:outline-primary-600 group flex flex-col gap-1.5 py-4 focus-visible:outline-2"
              >
                <span className="text-xs font-medium text-gray-500">
                  <span className="text-primary-600">{feed.type}</span> · {feed.categoryName}
                </span>
                <h3 className="text-base leading-snug font-bold tracking-tight text-gray-900 group-hover:underline">
                  {feed.title}
                </h3>
                <p className="line-clamp-3 text-sm leading-relaxed text-gray-600">{feed.content}</p>
                <span className="text-xs text-gray-400">
                  {feed.authorDisplay} · 답변 {feed.commentCount} · 좋아요 {feed.likeCount}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </aside>
  );
}
