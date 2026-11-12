import { useSuspenseQuery } from '@tanstack/react-query';

import { projectDetailQueryOptions } from '@/api/project-detail';
import { AsyncBoundary } from '@/components/AsyncBoundary';
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

  const memberUserIds = project.members.map((member) => member.userId);

  return (
    <>
      {/* <title> 안 글자가 여러 조각이면 React가 제목을 비워 두어 한 문자열로 넘긴다. */}
      <title>{`${project.title} | shout-outz`}</title>
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
          // 경험·자료도 빌드할 때 미리 그린다. 팀원에게만 보이는 제보 링크는 useIsProjectMember가 브라우저 렌더에서만 켠다.
          <AsyncBoundary>
            {tab === 'experience' ? (
              <ProjectExperiences slug={slug} memberUserIds={memberUserIds} />
            ) : (
              <ProjectMaterials
                slug={slug}
                projectName={project.title}
                memberUserIds={memberUserIds}
              />
            )}
          </AsyncBoundary>
        )}
      </div>
    </>
  );
}
