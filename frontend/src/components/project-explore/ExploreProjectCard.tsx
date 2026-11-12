import { Link } from '@tanstack/react-router';
import { useState } from 'react';

import type { ProjectListItem } from '@/types/project';
import { analytics } from '@/utils/analytics';
import { toProjectSlugParam } from '@/utils/project';

type ExploreProjectCardProps = Pick<
  ProjectListItem,
  'slug' | 'title' | 'tagline' | 'cohort' | 'likeCount' | 'thumbnailUrl'
>;

/** 탐색 프로젝트 탭 카드. 프로젝트 목록 카드보다 단순하게 썸네일, 기수, 이름, 소개, 좋아요 수만 보여 준다. */
export function ExploreProjectCard({
  slug,
  title,
  tagline,
  cohort,
  likeCount,
  thumbnailUrl,
}: ExploreProjectCardProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const thumbnail = thumbnailUrl?.trim();

  return (
    <Link
      to="/projects/$slug"
      params={{ slug: toProjectSlugParam(slug) }}
      className="focus-visible:outline-primary-600 group block rounded-xl focus-visible:outline-2 focus-visible:outline-offset-4"
      onClick={() => {
        analytics.track({ name: 'card_clicked', target: 'project', surface: 'explore' });
        analytics.track({ name: 'project_detail_opened', slug, from: 'explore' });
      }}
    >
      <div className="flex aspect-[8/5] items-center justify-center overflow-hidden rounded-xl bg-gray-100">
        {thumbnail && thumbnail !== failedUrl ? (
          <img
            src={thumbnail}
            alt={`${title} 썸네일`}
            loading="lazy"
            className="size-full object-cover transition-transform group-hover:scale-[1.02]"
            onError={() => setFailedUrl(thumbnail)}
          />
        ) : (
          <span aria-hidden="true" className="px-4 text-center text-sm font-semibold text-gray-500">
            {title}
          </span>
        )}
      </div>
      {cohort != null && <p className="mt-3 text-xs text-gray-500">우아한테크코스 {cohort}기</p>}
      <h3
        className={`text-lg leading-snug font-bold break-words text-gray-900 group-hover:underline ${cohort != null ? 'mt-0.5' : 'mt-3'}`}
      >
        {title}
      </h3>
      <p className="mt-1 line-clamp-2 text-sm leading-relaxed break-words text-gray-600">
        {tagline}
      </p>
      <p className="mt-2 text-xs text-gray-500">좋아요 {likeCount}</p>
    </Link>
  );
}
