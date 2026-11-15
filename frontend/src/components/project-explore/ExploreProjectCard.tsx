import { Link } from '@tanstack/react-router';
import { useState } from 'react';

import type { MatchedResource } from '@/apis/project-explore';
import { Badge } from '@/components/Badge';
import { sourceBadgeOf } from '@/components/explore/MaterialParts';
import type { ProjectListItem } from '@/types/project';
import { analytics } from '@/utils/analytics';
import { toProjectSlugParam } from '@/utils/project';

/**
 * 거른 목록에서 이 프로젝트가 나온 까닭.
 * - topic: 왼쪽 기술 주제로 걸렀거나 검색어를 주제로 해석했을 때의 주제 이름
 * - resource: 검색어로 찾았을 때 프로젝트가 걸린 근거 자료
 */
export type ExploreProjectMatch =
  | { kind: 'topic'; topic: string }
  | { kind: 'resource'; keyword: string; resource: MatchedResource };

type ExploreProjectCardProps = Pick<
  ProjectListItem,
  'slug' | 'title' | 'tagline' | 'cohort' | 'likeCount' | 'thumbnailUrl'
> & {
  /** 거르지 않은 목록이면 없다. */
  match?: ExploreProjectMatch | null;
};

/**
 * 탐색 프로젝트 탭 카드. 썸네일, 기수, 이름, 소개, 좋아요 수를 보여 주고,
 * 주제나 검색어로 거른 목록이면 소개 아래에 거른 까닭을 한 줄로 더한다.
 */
export function ExploreProjectCard({
  slug,
  title,
  tagline,
  cohort,
  likeCount,
  thumbnailUrl,
  match,
}: ExploreProjectCardProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const thumbnail = thumbnailUrl?.trim();

  return (
    <Link
      // 주제로 거른 목록이면 그 주제를 고른 자료 탭으로 바로 연다.
      {...(match?.kind === 'topic'
        ? { to: '/projects/$slug/materials', search: { topic: match.topic } }
        : { to: '/projects/$slug' })}
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
      {match && <MatchLine match={match} />}
      <p className="mt-2 text-xs text-gray-500">좋아요 {likeCount}</p>
    </Link>
  );
}

/** 카드 소개 아래 거른 까닭. 카드 전체가 링크라 안에는 누를 것을 두지 않는다. */
function MatchLine({ match }: { match: ExploreProjectMatch }) {
  if (match.kind === 'topic') {
    return (
      <p className="mt-3 flex items-center gap-1.5 text-xs text-gray-500">
        <Badge tone="primary">#{match.topic}</Badge>
        <span>주제를 다뤘어요</span>
      </p>
    );
  }

  const [label, tone] = sourceBadgeOf(match.resource.source);
  return (
    <p
      aria-label={`'${match.keyword}' 검색에 걸린 ${label}: ${match.resource.title}`}
      className="mt-3 flex min-w-0 items-center gap-2 rounded-lg bg-gray-50 px-2.5 py-2"
    >
      <Badge tone={tone} className="shrink-0">
        {label}
      </Badge>
      <span className="truncate text-xs text-gray-700">{match.resource.title}</span>
    </p>
  );
}
