import { useSuspenseQuery } from '@tanstack/react-query';
import { useId, useState } from 'react';

import {
  projectCareerCardsQuery,
  type CareerCardSource,
  type ProjectCareerCard,
} from '@/apis/project-explore';
import { DotTab } from '@/components/explore/DotTab';
import { isGithubMaterial, isPullRequest, sourceBadgeOf } from '@/components/explore/MaterialParts';
import { ResourceReportModal } from '@/components/projects/detail/ResourceReportModal';
import { useIsProjectMember } from '@/components/projects/detail/useIsProjectMember';
import { useModal } from '@/hooks/useModal';
import { cn } from '@/utils/cn';

// 서버는 직군을 BE, FE, AN 순으로 준다.
const JOB_ROLES: { jobRole: string; label: string }[] = [
  { jobRole: 'BE', label: '백엔드' },
  { jobRole: 'FE', label: '프론트엔드' },
  { jobRole: 'AN', label: '안드로이드' },
];

/** 출처 동그라미 안 글자. PR은 PR, GitHub 문서는 D, 블로그는 B */
const shortLabelOf = (source: CareerCardSource) => {
  if (isPullRequest(source)) return 'PR';
  if (isGithubMaterial(source)) return 'D';
  return 'B';
};

/** PR 주소 끝의 번호. 예: .../pull/513 → 513 */
const pullRequestNumberOf = (source: CareerCardSource) =>
  isPullRequest(source) ? (/\/pull\/(\d+)/.exec(source.url)?.[1] ?? null) : null;

/** 프로젝트 상세 경험 탭 */
export function ProjectExperiences({
  slug,
  memberUserIds,
}: {
  slug: string;
  /** 팀원만 빠진 자료를 알려 줄 수 있어 제보 링크를 보여 줄지 정한다. */
  memberUserIds: (number | null | undefined)[];
}) {
  const { data: experiences } = useSuspenseQuery(projectCareerCardsQuery(slug));
  const isMember = useIsProjectMember(memberUserIds);
  const { open } = useModal();
  const jobRoles = JOB_ROLES.filter(({ jobRole }) =>
    experiences.some((experience) => experience.jobRole === jobRole),
  );
  const [selected, setSelected] = useState(jobRoles[0]?.jobRole);

  if (experiences.length === 0) {
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
            <li key={experience.cardId} className="border-t border-gray-100 first:border-t-0">
              {/* 프로토타입처럼 첫 경험의 출처만 펼쳐 둔다. */}
              <ExperienceItem experience={experience} defaultOpen={index === 0} />
            </li>
          ))}
      </ul>
      {isMember && (
        <p className="mt-4 border-t border-gray-100 pt-6 text-sm text-gray-500">
          찾는 자료가 없나요?{' '}
          <button
            type="button"
            className="focus-visible:outline-primary-600 cursor-pointer rounded-sm font-semibold text-gray-700 hover:underline focus-visible:outline-2"
            onClick={() =>
              void open<void>((close) => (
                <ResourceReportModal kind="addition" slug={slug} onClose={() => close()} />
              ))
            }
          >
            빠진 PR·팀 문서 알려 주기 →
          </button>
        </p>
      )}
    </div>
  );
}

// 프로토타입은 경험 카드 전체가 "자세히 보기"지만 경험 카드는 눌러도 아무것도 열지 않는다. 출처는 펼쳐서 원문으로 간다.
function ExperienceItem({
  experience,
  defaultOpen,
}: {
  experience: ProjectCareerCard;
  defaultOpen: boolean;
}) {
  const [open, setOpen] = useState(defaultOpen);
  const sourcesId = useId();

  return (
    <article className="flex flex-col gap-2.5 py-7">
      <span className="text-sm font-medium text-gray-500">{experience.category}</span>
      <h3 className="text-lg font-bold text-gray-900">{experience.title}</h3>
      <ul className="flex flex-col gap-2">
        {experience.resumeBullets.map((bullet) => (
          <li key={bullet} className="flex gap-2.5 text-sm leading-relaxed text-gray-600">
            {/* DESIGN-TOKEN: 점을 글자 첫 줄 가운데에 맞추려고 줄 높이에서 점 크기를 뺀 절반만큼 내린다. */}
            <span
              aria-hidden="true"
              className="mt-[calc((1lh-4px)/2)] size-1 shrink-0 rounded-full bg-gray-400"
            />
            <span className="min-w-0">{bullet}</span>
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
                  key={source.resourceId}
                  source={source}
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
              {experience.sources.map((source) => (
                <li key={source.resourceId}>
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

function SourceLink({ source }: { source: CareerCardSource }) {
  const number = pullRequestNumberOf(source);
  return (
    <a
      href={source.url}
      target="_blank"
      rel="noopener noreferrer"
      className="focus-visible:outline-primary-600 flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm text-gray-600 hover:bg-gray-50 focus-visible:outline-2"
    >
      <SourceBadge source={source} />
      <span className="sr-only">{sourceBadgeOf(source.source)[0]}: </span>
      <span className="min-w-0 truncate">{source.title}</span>
      {number !== null && <span className="ml-auto shrink-0 text-xs text-gray-400">#{number}</span>}
    </a>
  );
}

function SourceBadge({ source, className }: { source: CareerCardSource; className?: string }) {
  return (
    <span
      aria-hidden="true"
      // DESIGN-TOKEN: 지름 20px 원 안에 PR 두 글자를 넣으려면 8px 글자가 필요해 토큰보다 작게 둔다.
      className={cn(
        'bg-primary-50 text-primary-600 flex size-5 shrink-0 items-center justify-center rounded-full text-[0.5rem] font-extrabold ring-2 ring-white',
        className,
      )}
    >
      {shortLabelOf(source)}
    </span>
  );
}
