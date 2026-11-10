import { useSuspenseQuery } from '@tanstack/react-query';

import { projectDetailQueryOptions } from '@/api/project-detail';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { ClientOnly } from '@/components/ClientOnly';
import { ProjectDetailHeader } from '@/components/projects/detail/ProjectDetailHeader';
import { ProjectExperiences } from '@/components/projects/detail/ProjectExperiences';
import { ProjectIntro } from '@/components/projects/detail/ProjectIntro';
import { ProjectMaterials } from '@/components/projects/detail/ProjectMaterials';
import { Tab } from '@/components/Tab';
import { ProjectNotApprovedError } from '@/errors/project';

export type ProjectDetailTab = 'experience' | 'material' | 'intro';

const TABS: { value: ProjectDetailTab; label: string }[] = [
  { value: 'experience', label: '경험' },
  { value: 'material', label: '자료' },
  { value: 'intro', label: '소개' },
];

interface ProjectDetailContentProps {
  slug: string;
  tab: ProjectDetailTab;
  onTabChange: (tab: ProjectDetailTab) => void;
}

export function ProjectDetailContent({ slug, tab, onTabChange }: ProjectDetailContentProps) {
  const { data: project } = useSuspenseQuery(projectDetailQueryOptions(slug));

  if (project.approvalStatus !== 'APPROVED') {
    throw new ProjectNotApprovedError();
  }

  return (
    <>
      <title>{project.title} | shout-outz</title>
      <meta name="description" content={project.tagline} />
      <ProjectDetailHeader project={project} />
      <Tab
        variant="underline"
        size="sm"
        value={tab}
        onChange={(value) => onTabChange(value as ProjectDetailTab)}
        aria-label="프로젝트 정보"
        className="mt-6 w-full border-b border-gray-200"
      >
        {TABS.map((item) => (
          <Tab.Item
            key={item.value}
            value={item.value}
            id={`project-tab-${item.value}`}
            aria-controls="project-tab-panel"
            className="-mb-px data-[status=active]:font-semibold"
          >
            {item.label}
          </Tab.Item>
        ))}
      </Tab>
      <div
        id="project-tab-panel"
        role="tabpanel"
        aria-labelledby={`project-tab-${tab}`}
        className="pt-3.5"
      >
        {tab === 'intro' ? (
          <ProjectIntro project={project} />
        ) : (
          // 경험·자료는 서버 API가 없어 MSW 목으로만 응답하므로, 정적 HTML을 만들 때는 비우고 브라우저에서만 불러온다.
          <ClientOnly>
            <AsyncBoundary>
              {tab === 'experience' ? (
                <ProjectExperiences slug={slug} />
              ) : (
                <ProjectMaterials slug={slug} projectName={project.title} />
              )}
            </AsyncBoundary>
          </ClientOnly>
        )}
      </div>
    </>
  );
}
