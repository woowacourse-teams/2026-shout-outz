import { useQuery, useSuspenseQuery } from '@tanstack/react-query';
import { startTransition, useState, type ReactNode } from 'react';

import {
  projectResourceTopicsQuery,
  projectTopicResourcesQuery,
  type MaterialJobRole,
} from '@/apis/project-explore';
import type { Material } from '@/apis/explore';
import { myResourceReportsQuery, RESOURCE_REMOVAL_MAX_COUNT } from '@/apis/resource-report';
import { Button } from '@/components/Button';
import { DotTab } from '@/components/explore/DotTab';
import { chipClass } from '@/components/explore/ExploreHero';
import {
  MaterialList,
  TechTalkList,
  type MaterialSelection,
} from '@/components/explore/MaterialList';
import { isGithubMaterial } from '@/components/explore/MaterialParts';
import { ResourceReportModal } from '@/components/projects/detail/ResourceReportModal';
import { useIsProjectMember } from '@/components/projects/detail/useIsProjectMember';
import { useModal } from '@/hooks/useModal';
import { cn } from '@/utils/cn';
import { getApiErrorMessage } from '@/utils/error';

const JOB_ROLE_LABELS: Record<MaterialJobRole, string> = {
  ALL: '전체',
  BE: '백엔드',
  FE: '프론트엔드',
  AN: '안드로이드',
};

interface ProjectMaterialsProps {
  slug: string;
  projectName: string;
  /** 팀원만 자료를 제보할 수 있어 제보 링크를 보여 줄지 정한다. */
  memberUserIds: (number | null | undefined)[];
}

const reportLinkClass =
  'focus-visible:outline-primary-600 cursor-pointer rounded-sm text-left hover:text-gray-900 hover:underline focus-visible:outline-2';

/** 프로젝트 상세 자료 탭. 직군과 기술 주제를 고르면 그 주제의 팀 자료를 보여 준다. */
export function ProjectMaterials({ slug, projectName, memberUserIds }: ProjectMaterialsProps) {
  const [jobRole, setJobRole] = useState<MaterialJobRole>('ALL');
  const isMember = useIsProjectMember(memberUserIds);
  const { open } = useModal();
  // 이미 "필요 없어요"로 보낸 자료는 다시 고르지 못하게 표시한다.
  const { data: myReports } = useQuery({ ...myResourceReportsQuery(slug), enabled: isMember });
  const reportedIds = new Set(
    (myReports ?? []).flatMap((report) =>
      report.kind === 'REMOVE' && report.resourceId != null ? [report.resourceId] : [],
    ),
  );
  // null이면 고르는 중이 아니다.
  const [selectedIds, setSelectedIds] = useState<Set<number> | null>(null);
  const selection: MaterialSelection | undefined = selectedIds
    ? {
        canSelect: (material) =>
          isGithubMaterial(material) && !reportedIds.has(material.resourceId),
        selectedIds,
        onToggle: (resourceId) =>
          setSelectedIds((prev) => {
            const next = new Set(prev);
            if (next.has(resourceId)) next.delete(resourceId);
            else if (next.size < RESOURCE_REMOVAL_MAX_COUNT) next.add(resourceId);
            return next;
          }),
      }
    : undefined;

  const openAddition = () =>
    void open<void>((close) => (
      <ResourceReportModal kind="addition" slug={slug} onClose={() => close()} />
    ));
  const openRemoval = (resourceIds: number[]) =>
    void open<void>((close) => (
      <ResourceReportModal
        kind="removal"
        slug={slug}
        resourceIds={resourceIds}
        onClose={() => close()}
        onDone={() => setSelectedIds(null)}
      />
    ));

  const reportLinks = isMember && (
    <ul className="flex flex-col gap-2 border-t border-gray-200 pt-4.5 text-xs text-gray-500">
      <li>
        <button
          type="button"
          className={reportLinkClass}
          aria-pressed={selectedIds !== null}
          onClick={() => setSelectedIds((prev) => (prev ? null : new Set()))}
        >
          {selectedIds ? '고르기 그만하기' : '필요 없는 자료 고르기'}
        </button>
      </li>
      <li>
        <button type="button" className={reportLinkClass} onClick={openAddition}>
          빠진 자료 알려 주기
        </button>
      </li>
    </ul>
  );
  const { data } = useSuspenseQuery(projectResourceTopicsQuery(slug, jobRole));
  const topics = data.fields.flatMap((field) => field.topics);
  const [chosenTopicId, setChosenTopicId] = useState<string | null>(null);
  // 처음이나 직군을 바꿔 고른 주제가 목록에 없으면 첫 주제를 보여 준다.
  const topic = topics.find((item) => item.topicId === chosenTopicId) ?? topics[0] ?? null;

  if (data.totalCount === 0) {
    return <p className="py-16 text-center text-gray-500">아직 모은 자료가 없어요.</p>;
  }

  return (
    <div>
      <div role="tablist" aria-label="직군" className="mb-9 flex flex-wrap gap-4.5">
        {data.jobRoles.map((item) => (
          <DotTab
            key={item.jobRole}
            selected={jobRole === item.jobRole}
            count={item.count}
            // 새 직군 주제를 불러오는 동안 지금 화면을 그대로 둔다.
            onSelect={() => startTransition(() => setJobRole(item.jobRole))}
          >
            {JOB_ROLE_LABELS[item.jobRole]}
          </DotTab>
        ))}
      </div>

      <div className="lg:flex lg:gap-14">
        <nav aria-label="주제" className="lg:w-52 lg:shrink-0">
          {/* 모바일에서는 긴 주제 목록 대신 가로로 넘기는 칩으로 고른다. */}
          <ul className="-mx-4 flex gap-2 overflow-x-auto px-4 pb-2 lg:hidden">
            {topics.map((item) => (
              <li key={item.topicId} className="shrink-0">
                <button
                  type="button"
                  aria-pressed={item.topicId === topic?.topicId}
                  data-status={item.topicId === topic?.topicId ? 'active' : 'inactive'}
                  className={chipClass}
                  onClick={() => setChosenTopicId(item.topicId)}
                >
                  {item.name}
                </button>
              </li>
            ))}
          </ul>
          <div className="hidden flex-col gap-5.5 lg:flex">
            {data.fields.map((field) => (
              <div key={field.name} className="flex flex-col gap-0.5">
                <p className="mb-1.5 text-xs font-semibold text-gray-400">{field.name}</p>
                {field.topics.map((item) => {
                  const selected = item.topicId === topic?.topicId;
                  return (
                    <button
                      key={item.topicId}
                      type="button"
                      aria-current={selected}
                      onClick={() => setChosenTopicId(item.topicId)}
                      className={cn(
                        'focus-visible:outline-primary-600 -ml-2.5 flex cursor-pointer items-start justify-between gap-2 border-l-2 py-1 pl-2.5 text-left text-sm focus-visible:outline-2',
                        selected
                          ? 'border-gray-900 font-semibold text-gray-900'
                          : 'border-transparent text-gray-600 hover:text-gray-900',
                      )}
                    >
                      <span className="min-w-0">{item.name}</span>
                      <span className="shrink-0 font-medium text-gray-400">{item.count}</span>
                    </button>
                  );
                })}
              </div>
            ))}
            {reportLinks}
          </div>
        </nav>

        <div className="mt-8 min-w-0 flex-1 lg:mt-0">
          <p className="text-sm text-gray-500">
            {projectName}의 자료 {data.totalCount} · 블로그 {data.kindCounts.blog} · PR{' '}
            {data.kindCounts.pr} · 팀 문서 {data.kindCounts.teamDoc}
          </p>
          {topic && (
            <TopicMaterials
              key={`${topic.topicId}-${jobRole}`}
              slug={slug}
              projectName={projectName}
              topic={topic}
              jobRole={jobRole}
              selection={selection}
              reportedIds={isMember ? reportedIds : undefined}
            />
          )}
          {/* 모바일에는 왼쪽 주제 목록이 없어 제보 링크를 자료 아래에 둔다. */}
          {reportLinks && <div className="mt-10 lg:hidden">{reportLinks}</div>}
        </div>
      </div>

      {selectedIds && (
        <div className="sticky bottom-4 z-20 mt-8 flex items-center justify-between gap-3 rounded-xl border border-gray-200 bg-white px-4 py-3 shadow-lg">
          <p role="status" className="text-sm text-gray-700">
            {selectedIds.size > 0
              ? `PR·팀 문서 ${selectedIds.size}개를 골랐어요`
              : '필요 없는 PR·팀 문서를 골라 주세요'}
          </p>
          <div className="flex shrink-0 gap-2">
            <Button variant="ghost" size="sm" onClick={() => setSelectedIds(null)}>
              취소
            </Button>
            <Button
              size="sm"
              disabled={selectedIds.size === 0}
              onClick={() => openRemoval([...selectedIds])}
            >
              제보하기
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

interface TopicMaterialsProps {
  slug: string;
  projectName: string;
  topic: { topicId: string; name: string };
  jobRole: MaterialJobRole;
  selection?: MaterialSelection;
  /** 내가 "필요 없어요"로 보낸 자료. 팀원이 아니면 없다. */
  reportedIds?: ReadonlySet<number>;
}

function TopicMaterials({
  slug,
  projectName,
  topic,
  jobRole,
  selection,
  reportedIds,
}: TopicMaterialsProps) {
  const { data, error, isPending } = useQuery(
    projectTopicResourcesQuery(slug, topic.topicId, jobRole),
  );

  return (
    <>
      <h2 className="mt-1.5 text-2xl font-extrabold text-gray-900">{topic.name}</h2>
      {isPending && (
        <p role="status" className="py-10 text-center text-sm text-gray-500">
          자료를 불러오는 중이에요.
        </p>
      )}
      {error && (
        <p role="alert" className="py-10 text-center text-sm text-gray-500">
          {getApiErrorMessage(error)}
        </p>
      )}
      {data && (
        <div className="mt-5 flex flex-col gap-14">
          <MaterialSection title={`${projectName}의 이야기`} count={data.resources.length}>
            <MaterialList
              materials={data.resources}
              selection={selection}
              renderExtra={(material: Material) =>
                reportedIds?.has(material.resourceId) && (
                  <p className="text-xs font-semibold text-gray-500">필요 없어요 보냄</p>
                )
              }
            />
          </MaterialSection>
          {data.talks.length > 0 && (
            <MaterialSection title="관련 테코톡" count={data.talks.length}>
              <div className="pt-4">
                <TechTalkList talks={data.talks} columns={3} />
              </div>
            </MaterialSection>
          )}
          {data.otherTeamResources.length > 0 && (
            <MaterialSection
              title="다른 팀의 이야기"
              count={data.otherTeamResources.length}
              description="같은 주제를 다룬 다른 팀의 자료예요."
            >
              <MaterialList materials={data.otherTeamResources} />
            </MaterialSection>
          )}
        </div>
      )}
    </>
  );
}

interface MaterialSectionProps {
  title: string;
  count: number;
  description?: string;
  children: ReactNode;
}

function MaterialSection({ title, count, description, children }: MaterialSectionProps) {
  return (
    <section aria-label={`${title} ${count}`}>
      <div className="flex flex-col gap-1 pb-1">
        <h3 className="text-lg font-bold text-gray-900">
          {title} <span className="font-medium text-gray-400">{count}</span>
        </h3>
        {description && <p className="text-sm text-gray-500">{description}</p>}
      </div>
      {children}
    </section>
  );
}
