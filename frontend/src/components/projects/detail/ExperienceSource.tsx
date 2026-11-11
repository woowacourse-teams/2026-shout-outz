import type { ExperienceSourceKind, ProjectExperienceSource } from '@/apis/project-explore';
import { cn } from '@/utils/cn';

export const SOURCE_BADGE: Record<ExperienceSourceKind, { short: string; label: string }> = {
  PR: { short: 'PR', label: 'PR' },
  TEAM_DOC: { short: 'D', label: '팀 문서' },
  BLOG: { short: 'B', label: '블로그' },
};

/** 경험 출처 원문 링크 한 줄 */
export function SourceLink({ source }: { source: ProjectExperienceSource }) {
  return (
    <a
      href={source.url}
      target="_blank"
      rel="noopener noreferrer"
      className="focus-visible:outline-primary-600 flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm text-gray-600 hover:bg-gray-50 focus-visible:outline-2"
    >
      <SourceBadge kind={source.kind} />
      <span className="sr-only">{SOURCE_BADGE[source.kind].label}: </span>
      <span className="min-w-0 truncate">{source.title}</span>
      {source.number !== null && (
        <span className="ml-auto shrink-0 text-xs text-gray-400">#{source.number}</span>
      )}
    </a>
  );
}

export function SourceBadge({
  kind,
  className,
}: {
  kind: ExperienceSourceKind;
  className?: string;
}) {
  return (
    <span
      aria-hidden="true"
      // DESIGN-TOKEN: 지름 20px 원 안에 PR 두 글자를 넣으려면 8px 글자가 필요해 토큰보다 작게 둔다.
      className={cn(
        'bg-primary-50 text-primary-600 flex size-5 shrink-0 items-center justify-center rounded-full text-[0.5rem] font-extrabold ring-2 ring-white',
        className,
      )}
    >
      {SOURCE_BADGE[kind].short}
    </span>
  );
}
