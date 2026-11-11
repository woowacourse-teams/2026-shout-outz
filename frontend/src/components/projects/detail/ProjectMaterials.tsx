import { useQuery, useSuspenseQuery } from '@tanstack/react-query';
import { startTransition, useState, type ReactNode } from 'react';

import {
  projectMaterialTopicsQuery,
  projectTopicMaterialsQuery,
  type MaterialJobRole,
} from '@/apis/project-explore';
import { DotTab } from '@/components/explore/DotTab';
import { chipClass } from '@/components/explore/ExploreHero';
import { MaterialList, TechTalkList } from '@/components/explore/MaterialList';
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
}

/** 프로젝트 상세 자료 탭. 직군과 기술 주제를 고르면 그 주제의 팀 자료를 보여 준다. */
export function ProjectMaterials({ slug, projectName }: ProjectMaterialsProps) {
  const [jobRole, setJobRole] = useState<MaterialJobRole>('ALL');
  const { data } = useSuspenseQuery(projectMaterialTopicsQuery(slug, jobRole));
  const topics = data.fields.flatMap((field) => field.topics.map((topic) => topic.name));
  const [chosenTopic, setChosenTopic] = useState<string | null>(null);
  // 처음이나 직군을 바꿔 고른 주제가 목록에 없으면 첫 주제를 보여 준다.
  const topic = chosenTopic && topics.includes(chosenTopic) ? chosenTopic : (topics[0] ?? null);

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
            {topics.map((name) => (
              <li key={name} className="shrink-0">
                <button
                  type="button"
                  aria-pressed={name === topic}
                  data-status={name === topic ? 'active' : 'inactive'}
                  className={chipClass}
                  onClick={() => setChosenTopic(name)}
                >
                  {name}
                </button>
              </li>
            ))}
          </ul>
          <div className="hidden flex-col gap-5.5 lg:flex">
            {data.fields.map((field) => (
              <div key={field.name} className="flex flex-col gap-0.5">
                <p className="mb-1.5 text-xs font-semibold text-gray-400">{field.name}</p>
                {field.topics.map((item) => {
                  const selected = item.name === topic;
                  return (
                    <button
                      key={item.name}
                      type="button"
                      aria-current={selected}
                      onClick={() => setChosenTopic(item.name)}
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
            {/* MOCK-API: 제보 API가 생기면 그 API를 보고 모달이나 링크로 연결한다. 지금은 문구만 둔다. */}
            <ul className="flex flex-col gap-2 border-t border-gray-200 pt-4.5 text-xs text-gray-500">
              <li>제보 전에 읽어 주세요</li>
              <li>필요 없는 자료 고르기</li>
              <li>빠진 자료 알려 주기</li>
            </ul>
          </div>
        </nav>

        <div className="mt-8 min-w-0 flex-1 lg:mt-0">
          <p className="text-sm text-gray-500">
            {projectName}의 자료 {data.totalCount} · 블로그 {data.kindCounts.blog} · PR{' '}
            {data.kindCounts.pr} · 팀 문서 {data.kindCounts.teamDoc}
          </p>
          {topic && (
            <TopicMaterials key={topic} slug={slug} projectName={projectName} topic={topic} />
          )}
        </div>
      </div>
    </div>
  );
}

function TopicMaterials({ slug, projectName, topic }: ProjectMaterialsProps & { topic: string }) {
  const { data, error, isPending } = useQuery(projectTopicMaterialsQuery(slug, topic));

  return (
    <>
      <h2 className="mt-1.5 text-2xl font-extrabold text-gray-900">{topic}</h2>
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
          <MaterialSection title={`${projectName}의 이야기`} count={data.materials.length}>
            <MaterialList materials={data.materials} />
          </MaterialSection>
          {data.techTalks.length > 0 && (
            <MaterialSection title="관련 테코톡" count={data.techTalks.length}>
              <div className="pt-4">
                <TechTalkList talks={data.techTalks} columns={3} />
              </div>
            </MaterialSection>
          )}
          {data.otherTeamMaterials.length > 0 && (
            <MaterialSection
              title="다른 팀의 이야기"
              count={data.otherTeamMaterials.length}
              description="같은 주제를 다룬 다른 팀의 자료예요."
            >
              <MaterialList materials={data.otherTeamMaterials} />
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
