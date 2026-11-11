import { useSuspenseQuery } from '@tanstack/react-query';
import { Link, useRouterState } from '@tanstack/react-router';
import { useState } from 'react';

import { projectDetailQueryOptions } from '@/api/project-detail';
import { projectCareerCardsQuery, type ProjectCareerCard } from '@/apis/project-explore';
import { Badge } from '@/components/Badge';
import { MaterialList } from '@/components/explore/MaterialList';
import { BugReportModal } from '@/components/modals/BugReportModal';
import { ProjectNotApprovedError } from '@/errors/project';
import { useModal } from '@/hooks/useModal';
import { cn } from '@/utils/cn';
import { toProjectSlugParam } from '@/utils/project';

/** 다른 경험은 3개까지 보여 주고 나머지는 "모두 보기"로 프로젝트 상세 경험 탭에 보낸다. */
const OTHER_EXPERIENCE_COUNT = 3;

/**
 * 서버 소제목(template별)을 왼쪽 칸의 짧은 이름과 점 색으로 바꾼다.
 * 서버가 새 소제목을 보내면 소제목을 그대로 쓰고 회색 점을 붙인다.
 */
const SECTION_LABELS: Record<string, { label: string; dot: string }> = {
  '해결하려던 문제': { label: '문제', dot: 'bg-red-400' },
  목표: { label: '목표', dot: 'bg-red-400' },
  원인: { label: '원인', dot: 'bg-orange-300' },
  '고려한 방법': { label: '검토', dot: 'bg-gray-400' },
  '선택한 방법과 이유': { label: '선택', dot: 'bg-gray-400' },
  '해결 과정': { label: '해결', dot: 'bg-blue-500' },
  '적용한 내용': { label: '해결', dot: 'bg-blue-500' },
  '구현한 내용': { label: '구현', dot: 'bg-blue-500' },
  '확인한 결과': { label: '결과', dot: 'bg-emerald-500' },
  '측정 범위와 고려한 점': { label: '한계', dot: 'bg-gray-400' },
};

const SECTION_TITLE = 'mb-3 text-lg font-bold tracking-tight text-gray-900';
const UNDERLINE_LINK =
  'underline decoration-gray-200 underline-offset-3 hover:decoration-gray-500 focus-visible:outline-primary-600 rounded-sm focus-visible:outline-2';

/** 경험 상세(포트폴리오). 프로젝트 상세 경험 탭에서 경험 카드를 누르면 온다. */
export function ExperienceDetail({ slug, cardId }: { slug: string; cardId: number }) {
  const { data: project } = useSuspenseQuery(projectDetailQueryOptions(slug));
  const { data: experiences } = useSuspenseQuery(projectCareerCardsQuery(slug));

  if (project.approvalStatus !== 'APPROVED') throw new ProjectNotApprovedError();

  const experience = experiences.find((item) => item.cardId === cardId);
  const projectParams = { slug: toProjectSlugParam(slug) };

  if (!experience) {
    return (
      <div role="alert" className="flex flex-col items-center gap-4 py-16 text-center">
        <title>경험을 찾을 수 없음 | shout-outz</title>
        <h1 className="text-xl font-bold">경험을 찾을 수 없어요.</h1>
        <Link to="/projects/$slug" params={projectParams} className={UNDERLINE_LINK}>
          {project.title}의 경험 보기 →
        </Link>
      </div>
    );
  }

  // 같은 직군 경험을 먼저 보여 준다.
  const others = experiences
    .filter((item) => item.cardId !== cardId)
    .sort(
      (a, b) => Number(b.jobRole === experience.jobRole) - Number(a.jobRole === experience.jobRole),
    );

  return (
    <div className="flex flex-col gap-10">
      <title>{`${experience.title} | ${project.title} | shout-outz`}</title>
      <meta name="description" content={experience.resumeBullets[0] ?? project.tagline} />

      <div className="flex flex-col">
        <p className="flex flex-wrap items-center gap-1.5 text-sm font-semibold text-gray-500">
          <Link
            to="/projects/$slug"
            params={projectParams}
            aria-label={`${project.title} 프로젝트로 이동`}
            className="inline-flex items-center gap-1.5 text-gray-600"
          >
            <ProjectLogo imageUrl={project.imageUrl} />
            <span className={UNDERLINE_LINK}>{project.title}</span>
          </Link>
          <span aria-hidden="true" className="font-normal text-gray-400">
            /
          </span>
          <span>{experience.category}</span>
        </p>
        <h1 className="mt-2.5 text-2xl leading-snug font-extrabold tracking-tight text-gray-900 md:text-3xl">
          {experience.title}
        </h1>
      </div>

      {experience.techs.length > 0 && (
        <section>
          <h2 className={SECTION_TITLE}>기술 태그</h2>
          <ul aria-label="기술 태그" className="flex flex-wrap gap-1.5">
            {experience.techs.map((tech) => (
              <li key={tech}>
                <Badge>{tech}</Badge>
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* PROTOTYPE-DIFF: 프로토타입의 "이 경험에서 볼 수 있는 점"(번호 목록)은 서버 응답에 해당 값이 없어 뺐다. 이력서 문장과는 다른 글이라 대신 쓰지 않는다. */}

      {experience.sections.length > 0 && (
        <section>
          <h2 className={SECTION_TITLE}>어떻게 풀었나</h2>
          <dl className="border-t border-gray-100 py-2">
            {experience.sections.map((section) => {
              const meta = SECTION_LABELS[section.heading];
              return (
                // DESIGN-TOKEN: 프로토타입의 왼쪽 이름 칸 56px. 그리드 칸 너비는 토큰으로 쓸 수 없다.
                <div key={section.heading} className="grid grid-cols-[3.5rem_1fr] gap-3 py-1.5">
                  <dt className="inline-flex h-6 items-center gap-1.5 text-xs font-bold text-gray-600">
                    <span
                      aria-hidden="true"
                      className={cn('size-1.5 shrink-0 rounded-full', meta?.dot ?? 'bg-gray-400')}
                    />
                    {meta?.label ?? section.heading}
                  </dt>
                  <dd className="text-base leading-relaxed text-gray-600">{section.body}</dd>
                </div>
              );
            })}
          </dl>
        </section>
      )}

      {experience.sources.length > 0 && (
        <section>
          <h2 className={SECTION_TITLE}>
            연관 자료 <span className="font-medium text-gray-400">{experience.sources.length}</span>
          </h2>
          <MaterialList materials={experience.sources} />
        </section>
      )}

      {others.length > 0 && (
        <section>
          <h2 className={SECTION_TITLE}>
            {project.title}의 다른 경험{' '}
            <span className="font-medium text-gray-400">{others.length}</span>
          </h2>
          <ul>
            {others.slice(0, OTHER_EXPERIENCE_COUNT).map((other) => (
              <li key={other.cardId} className="border-t border-gray-100">
                <OtherExperienceLink slug={slug} experience={other} />
              </li>
            ))}
          </ul>
          <p className="mt-5 text-center">
            <Link
              to="/projects/$slug"
              params={projectParams}
              className={cn(UNDERLINE_LINK, 'text-sm font-semibold text-gray-600')}
            >
              {project.title}의 경험 {experiences.length}개 모두 보기 →
            </Link>
          </p>
        </section>
      )}

      <ReportPrompt />
    </div>
  );
}

function OtherExperienceLink({
  slug,
  experience,
}: {
  slug: string;
  experience: ProjectCareerCard;
}) {
  return (
    <Link
      to="/projects/$slug/experiences/$cardId"
      params={{ slug: toProjectSlugParam(slug), cardId: String(experience.cardId) }}
      className="group focus-visible:outline-primary-600 flex flex-col gap-2 py-5.5 focus-visible:outline-2"
    >
      <span className="text-sm text-gray-400">{experience.category}</span>
      <h3 className="text-lg leading-snug font-bold tracking-tight text-gray-900 group-hover:underline">
        {experience.title}
      </h3>
      {experience.resumeBullets[0] && (
        <p className="text-base leading-relaxed text-gray-800">{experience.resumeBullets[0]}</p>
      )}
      {experience.sources.length > 0 && (
        <span className="text-xs text-gray-400">출처 {experience.sources.length}개</span>
      )}
    </Link>
  );
}

/** 이름 앞 16px 프로젝트 대표 이미지. 없거나 열리지 않으면 그리지 않는다. */
function ProjectLogo({ imageUrl }: { imageUrl?: string | null }) {
  const [failed, setFailed] = useState(false);
  const src = imageUrl?.trim();
  if (!src || failed) return null;
  return (
    <img
      src={src}
      alt=""
      width={16}
      height={16}
      className="size-4 shrink-0 rounded-sm object-cover"
      onError={() => setFailed(true)}
    />
  );
}

/** 맨 아래 제보 안내. 버그 제보 창을 지금 화면 경로와 함께 연다. */
function ReportPrompt() {
  const { open } = useModal();
  const pathname = useRouterState({ select: (state) => state.location.pathname });

  return (
    <p className="border-t border-gray-100 pt-7 text-center text-sm text-gray-500">
      빠진 자료나 잘못된 내용이 있나요?{' '}
      {/* ASSUMPTION: 프로젝트 팀원이 아니어도 누구나 알릴 수 있게 팀원 전용 자료 제보 대신 버그 제보 창을 연다. */}
      <button
        type="button"
        onClick={() =>
          void open<void>((close) => <BugReportModal pagePath={pathname} onClose={() => close()} />)
        }
        className={cn(UNDERLINE_LINK, 'cursor-pointer font-bold text-gray-900')}
      >
        제보하기 →
      </button>
    </p>
  );
}
