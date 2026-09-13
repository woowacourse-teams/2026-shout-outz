import { useState, useSyncExternalStore } from 'react';
import { useQuery, useSuspenseQuery } from '@tanstack/react-query';
import {
  projectDetailQueryOptions,
  projectReactionsQueryOptions,
  type ProjectDetail,
} from '@/api/project-detail';
import { Avatar } from '@/components/Avatar';
import { Badge } from '@/components/Badge';
import { getButtonStyles } from '@/components/Button';
import { MarkdownContent } from '@/components/MarkdownContent';

const subscribe = () => () => {};
const clientSnapshot = () => true;
const serverSnapshot = () => false;

function ProjectReactions({ project }: { project: ProjectDetail }) {
  const hydrated = useSyncExternalStore(subscribe, clientSnapshot, serverSnapshot);
  const { data, isError } = useQuery({
    ...projectReactionsQueryOptions(String(project.id)),
    enabled: hydrated,
  });
  const ready = hydrated && data && !isError;
  return (
    <div className="flex flex-wrap gap-2" aria-label="프로젝트 반응">
      <span className="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3 text-sm text-gray-600">
        좋아요 {ready ? data.likeCount : project.likeCount}
        <span className="ml-2 text-xs text-gray-500">
          {ready
            ? data.likedByMe
              ? '좋아요 함'
              : '좋아요 안 함'
            : isError
              ? '상태 확인 불가'
              : '상태 확인 중'}
        </span>
      </span>
      <span className="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3 text-sm text-gray-600">
        저장 {ready ? data.bookmarkCount : project.bookmarkCount}
        <span className="ml-2 text-xs text-gray-500">
          {ready
            ? data.bookmarkedByMe
              ? '저장함'
              : '저장 안 함'
            : isError
              ? '상태 확인 불가'
              : '상태 확인 중'}
        </span>
      </span>
    </div>
  );
}

export function ProjectDetailContent({ projectId }: { projectId: string }) {
  const { data: project } = useSuspenseQuery(projectDetailQueryOptions(projectId));
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const thumbnail = project.thumbnailUrl?.trim();
  const links = [
    { label: '서비스 바로가기 ↗', url: project.deploymentUrl?.trim(), variant: 'primary' as const },
    {
      label: 'GitHub 저장소 ↗',
      url: project.githubRepositoryUrl?.trim(),
      variant: 'secondary' as const,
    },
  ].filter((link) => link.url);

  return (
    <>
      <title>{project.title} | shout-outz</title>
      <meta name="description" content={project.tagline} />
      <section aria-label="프로젝트 요약" className="grid min-w-0 gap-6 lg:grid-cols-3 lg:gap-9">
        <div className="flex aspect-video items-center justify-center overflow-hidden rounded-2xl bg-gray-100">
          {thumbnail && thumbnail !== failedUrl ? (
            <img
              src={thumbnail}
              alt={`${project.title} 대표 이미지`}
              className="size-full object-cover"
              onError={() => setFailedUrl(thumbnail)}
            />
          ) : (
            <div className="space-y-3 px-6 text-center">
              <span
                aria-hidden="true"
                className="bg-primary-600 mx-auto flex size-12 items-center justify-center rounded-xl text-2xl font-bold text-white"
              >
                {project.title.slice(0, 1)}
              </span>
              <p className="font-bold">{project.teamName || project.title}</p>
            </div>
          )}
        </div>
        <div className="min-w-0 self-center lg:col-span-2">
          <p className="text-primary-600 text-sm font-semibold">
            우아한테크코스 {project.cohort}기 ·{' '}
            {project.serviceStatus === 'OPERATING' ? '운영 중' : project.serviceStatus}
          </p>
          <h1 className="mt-3 text-3xl font-bold break-words">{project.title}</h1>
          <p className="mt-4 text-base leading-relaxed break-words text-gray-600">
            {project.tagline}
          </p>
          <div className="mt-5 flex flex-wrap items-center gap-3">
            {links.map((link) => (
              <a
                key={link.label}
                href={link.url}
                target="_blank"
                rel="noopener noreferrer"
                className={getButtonStyles({ variant: link.variant, size: 'lg' })}
              >
                {link.label}
              </a>
            ))}
            <ProjectReactions project={project} />
          </div>
        </div>
      </section>
      <div className="mt-10 grid min-w-0 gap-10 pb-10 lg:mt-16 lg:grid-cols-3 lg:gap-12 lg:pb-20">
        <section aria-label="프로젝트 소개" className="min-w-0 lg:col-span-2">
          <MarkdownContent>{project.descriptionMd}</MarkdownContent>
        </section>
        <aside className="min-w-0 space-y-10">
          <section aria-labelledby="project-members">
            <h2 id="project-members" className="text-lg font-bold">
              참여 크루 ({project.members.length}명)
            </h2>
            {project.members.length ? (
              <ul className="mt-4 space-y-3">
                {project.members.map((member, index) => (
                  <li key={member.userId} className="flex items-center gap-3">
                    <Avatar src={member.avatarUrl ?? undefined} alt="" />
                    <p className="text-sm font-semibold break-words">
                      {member.displayName}
                      {index === 0 ? ' (작성자)' : ''} · {member.cohort}기{' '}
                      {({ BE: '백엔드', FE: '프론트엔드' } as Record<string, string>)[
                        member.track
                      ] ?? member.track}
                    </p>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-4 text-sm text-gray-500">등록된 참여 크루가 없습니다.</p>
            )}
          </section>
          <section aria-labelledby="project-tech-tags">
            <h2 id="project-tech-tags" className="text-lg font-bold">
              기술 스택
            </h2>
            {project.techTags.length ? (
              <div className="mt-4 flex flex-wrap gap-2">
                {project.techTags.map((tag) => (
                  <Badge key={tag.id}>{tag.displayName}</Badge>
                ))}
              </div>
            ) : (
              <p className="mt-4 text-sm text-gray-500">등록된 기술 스택이 없습니다.</p>
            )}
          </section>
        </aside>
      </div>
    </>
  );
}
