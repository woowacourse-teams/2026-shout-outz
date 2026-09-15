import { useState } from 'react';
import type { ProjectListItem } from '@/api/project-list';

export function ProjectCard({ project }: { project: ProjectListItem }) {
  const [failedUrl, setFailedUrl] = useState<string | null>();
  const showImage = project.thumbnailUrl && project.thumbnailUrl !== failedUrl;

  return (
    <article className="min-w-0">
      <div className="flex aspect-video items-center justify-center overflow-hidden rounded-xl bg-gray-100">
        {showImage ? (
          <img
            src={project.thumbnailUrl!}
            alt=""
            loading="lazy"
            onError={() => setFailedUrl(project.thumbnailUrl)}
            className="size-full object-cover"
          />
        ) : (
          <span aria-hidden="true" className="px-4 text-center text-sm font-semibold text-gray-500">
            {project.title}
          </span>
        )}
      </div>
      <p className="mt-3 text-xs text-gray-500">우아한테크코스 {project.cohort}기</p>
      <h2 className="mt-2 text-lg font-bold break-words text-gray-900">{project.title}</h2>
      <p className="mt-2 text-sm leading-relaxed break-words text-gray-600">{project.tagline}</p>
    </article>
  );
}
