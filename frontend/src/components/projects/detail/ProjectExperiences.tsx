import { useSuspenseQuery } from '@tanstack/react-query';
import { useId, useState } from 'react';

import {
  projectExperiencesQuery,
  type ExperienceJobRole,
  type ExperienceSourceKind,
  type ProjectExperience,
  type ProjectExperienceSource,
} from '@/apis/project-explore';
import { DotTab } from '@/components/explore/DotTab';
import { cn } from '@/utils/cn';

const JOB_ROLES: { jobRole: ExperienceJobRole; label: string }[] = [
  { jobRole: 'BE', label: '백엔드' },
  { jobRole: 'FE', label: '프론트엔드' },
  { jobRole: 'AN', label: '안드로이드' },
];

const SOURCE_BADGE: Record<ExperienceSourceKind, { short: string; label: string }> = {
  PR: { short: 'PR', label: 'PR' },
  TEAM_DOC: { short: 'D', label: '팀 문서' },
  BLOG: { short: 'B', label: '블로그' },
};

/** 프로젝트 상세 경험 탭 */
export function ProjectExperiences({ slug }: { slug: string }) {
  const { data: experiences } = useSuspenseQuery(projectExperiencesQuery(slug));
  const jobRoles = JOB_ROLES.filter(({ jobRole }) =>
    experiences.some((experience) => experience.jobRole === jobRole),
  );
  const [selected, setSelected] = useState(jobRoles[0]?.jobRole);

  if (experiences.length === 0) {
    // ASSUMPTION: 프로토타입에 경험이 없는 화면이 없어 안내 문구를 둔다.
    return <p className="py-16 text-center text-gray-500">아직 정리된 경험이 없어요.</p>;
  }

  return (
    <div>
      <div role="tablist" aria-label="직군" className="mb-5 flex flex-wrap gap-4.5">
        {jobRoles.map(({ jobRole, label }) => (
          <DotTab
            key={jobRole}
            selected={selected === jobRole}
            onSelect={() => setSelected(jobRole)}
          >
            {label}
          </DotTab>
        ))}
      </div>
      <ul aria-label="경험">
        {experiences
          .filter((experience) => experience.jobRole === selected)
          .map((experience, index) => (
            <li key={experience.experienceId} className="border-t border-gray-100 first:border-t-0">
              {/* 프로토타입처럼 첫 경험의 출처만 펼쳐 둔다. */}
              <ExperienceItem experience={experience} defaultOpen={index === 0} />
            </li>
          ))}
      </ul>
      {/* ASSUMPTION: 제보 기능이 아직 없어 "빠진 PR·팀 문서 알려 주기"는 링크 없이 문구만 둔다. */}
      <p className="mt-4 border-t border-gray-100 pt-6 text-sm text-gray-500">
        찾는 자료가 없나요?{' '}
        <span className="font-semibold text-gray-700">빠진 PR·팀 문서 알려 주기 →</span>
      </p>
    </div>
  );
}

// ASSUMPTION: 프로토타입은 경험 카드 전체가 "자세히 보기" 링크지만 경험 상세 화면이 없어 링크를 달지 않는다. 출처는 펼쳐서 원문으로 간다.
function ExperienceItem({
  experience,
  defaultOpen,
}: {
  experience: ProjectExperience;
  defaultOpen: boolean;
}) {
  const [open, setOpen] = useState(defaultOpen);
  const sourcesId = useId();

  return (
    <article className="flex flex-col gap-2.5 py-7">
      <span className="text-sm font-medium text-gray-500">{experience.category}</span>
      <h3 className="text-lg font-bold text-gray-900">{experience.title}</h3>
      <ul className="flex flex-col gap-2">
        {experience.highlights.map((highlight) => (
          <li key={highlight} className="flex gap-2.5 text-sm leading-relaxed text-gray-600">
            {/* DESIGN-TOKEN: 점을 글자 첫 줄 가운데에 맞추려고 줄 높이에서 점 크기를 뺀 절반만큼 내린다. */}
            <span
              aria-hidden="true"
              className="mt-[calc((1lh-4px)/2)] size-1 shrink-0 rounded-full bg-gray-400"
            />
            <span className="min-w-0">{highlight}</span>
          </li>
        ))}
      </ul>
      {experience.sources.length > 0 && (
        <div className="mt-1.5 flex flex-col items-start gap-2">
          <button
            type="button"
            aria-expanded={open}
            aria-controls={sourcesId}
            onClick={() => setOpen((value) => !value)}
            className="focus-visible:outline-primary-600 inline-flex cursor-pointer items-center gap-2 rounded-full border border-gray-200 bg-white py-1 pr-3 pl-1 text-sm font-semibold text-gray-600 hover:bg-gray-50 focus-visible:outline-2"
          >
            <span aria-hidden="true" className="flex">
              {experience.sources.map((source, index) => (
                <SourceBadge
                  key={index}
                  kind={source.kind}
                  className={cn(index > 0 && '-ml-1.5')}
                />
              ))}
            </span>
            출처 {experience.sources.length}개
            <span aria-hidden="true" className="text-xs text-gray-400">
              {open ? '▴' : '▾'}
            </span>
          </button>
          {open && (
            <ul
              id={sourcesId}
              className="flex w-full max-w-140 flex-col rounded-xl border border-gray-200 bg-white p-1.5 shadow-md"
            >
              {experience.sources.map((source, index) => (
                <li key={index}>
                  <SourceLink source={source} />
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </article>
  );
}

function SourceLink({ source }: { source: ProjectExperienceSource }) {
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

function SourceBadge({ kind, className }: { kind: ExperienceSourceKind; className?: string }) {
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
