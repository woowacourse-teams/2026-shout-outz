import { Link } from '@tanstack/react-router';
import { useState } from 'react';

import { ProjectLikeButton } from '@/components/projects/ProjectLikeButton';
import type { ProjectDetail } from '@/types/project';
import { toProjectSlugParam } from '@/utils/project';

/** 프로젝트 상세 위쪽. 대표 이미지, 이름, 한 줄 소개, 링크 */
export function ProjectDetailHeader({ project }: { project: ProjectDetail }) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const thumbnail = project.imageUrl?.trim();
  const links = [
    { label: '서비스 바로가기 ↗', url: project.deploymentUrl?.trim() },
    { label: 'GitHub 저장소 ↗', url: project.githubRepositoryUrl?.trim() },
  ].filter((link) => link.url);

  return (
    <div className="flex items-start gap-3.5 md:items-center md:gap-7">
      <div className="flex size-16 shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-gray-100 md:size-28">
        {thumbnail && thumbnail !== failedUrl ? (
          <img
            src={thumbnail}
            alt={`${project.title} 대표 이미지`}
            className="size-full object-cover"
            onError={() => setFailedUrl(thumbnail)}
          />
        ) : (
          <span role="img" aria-label={`${project.title} 대표 이미지`} className="size-full" />
        )}
      </div>
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <h1 className="text-2xl leading-tight font-extrabold break-words text-gray-900 md:text-3xl">
          {project.title}
        </h1>
        <p className="text-sm break-words text-gray-600 md:text-base">{project.tagline}</p>
        {(links.length > 0 || project.editable) && (
          <div className="mt-0.5 flex flex-wrap gap-x-5 gap-y-1">
            {links.map((link) => (
              <a
                key={link.label}
                href={link.url}
                target="_blank"
                rel="noopener noreferrer"
                className={LINK_CLASS_NAME}
              >
                {link.label}
              </a>
            ))}
            {/* PROTOTYPE-DIFF: 프로토타입에는 없지만 참여 크루가 수정할 수 있게 링크 줄 끝에 둔다. */}
            {project.editable && (
              <Link
                to="/projects/$slug/edit"
                params={{ slug: toProjectSlugParam(project.slug) }}
                className={LINK_CLASS_NAME}
              >
                프로젝트 수정
              </Link>
            )}
          </div>
        )}
      </div>
      <ProjectLikeButton
        slug={project.slug}
        likeCount={project.likeCount}
        likedByMe={project.likedByMe}
      />
    </div>
  );
}

const LINK_CLASS_NAME =
  'focus-visible:outline-primary-600 rounded-sm text-sm whitespace-nowrap text-gray-600 underline decoration-gray-200 underline-offset-3 hover:text-gray-900 hover:decoration-gray-400 focus-visible:outline-2';
