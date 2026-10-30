import { Link } from '@tanstack/react-router';

import { ProjectCard } from '@/components/projects/ProjectCard';
import { type ProjectSummary } from '@/types/project';
import { analytics } from '@/utils/analytics';
import { toProjectSlugParam } from '@/utils/project';

export interface ProjectCardLinkProps {
  project: ProjectSummary;
  /** 카드를 누른 화면. analytics의 `surface`와 `from`으로 보낸다. */
  surface: string;
}

export function ProjectCardLink({ project, surface }: ProjectCardLinkProps) {
  return (
    <Link
      to="/projects/$slug"
      params={{ slug: toProjectSlugParam(project.slug) }}
      className="focus-visible:outline-primary-600 block rounded-xl focus-visible:outline-2"
      onClick={() => {
        analytics.track({ name: 'card_clicked', target: 'project', surface });
        analytics.track({ name: 'project_detail_opened', slug: project.slug, from: surface });
      }}
    >
      <ProjectCard
        title={project.title}
        tagline={project.tagline}
        cohort={project.cohort}
        likeCount={project.likeCount}
        likedByMe={project.likedByMe}
        commentCount={project.commentCount}
        techTags={project.techTags}
        thumbnailUrl={project.thumbnailUrl}
        members={project.members}
      />
    </Link>
  );
}
