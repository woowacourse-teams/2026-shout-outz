import { Link } from '@tanstack/react-router';

import { Avatar } from '@/components/Avatar';
import { Badge } from '@/components/Badge';
import { MarkdownContent } from '@/components/MarkdownContent';
import { UserAffiliation } from '@/components/users/UserAffiliation';
import { UserHandle } from '@/components/users/UserHandle';
import type { ProjectDetail } from '@/types/project';

/** 프로젝트 상세 소개 탭. 소개 글과 참여 크루·기술 스택 */
export function ProjectIntro({ project }: { project: ProjectDetail }) {
  return (
    <div className="flex flex-col gap-12 pt-8 lg:flex-row lg:gap-16">
      <section aria-label="프로젝트 소개" className="min-w-0 flex-1">
        <MarkdownContent>{project.descriptionMd ?? ''}</MarkdownContent>
      </section>
      <aside className="flex min-w-0 flex-col gap-9 lg:w-62 lg:shrink-0">
        <section aria-labelledby="project-members">
          <h2 id="project-members" className="pb-2.5 text-sm font-bold text-gray-900">
            참여 크루 <span className="font-medium text-gray-400">{project.members.length}</span>
          </h2>
          {project.members.length ? (
            <ul className="flex flex-col">
              {project.members.map((member, index) => (
                <ProjectMemberProfile
                  key={member.handle ?? `${member.displayName}-${index}`}
                  member={member}
                />
              ))}
            </ul>
          ) : (
            <p className="text-sm text-gray-500">등록된 참여 크루가 없습니다.</p>
          )}
        </section>
        <section aria-labelledby="project-tech-tags">
          <h2 id="project-tech-tags" className="pb-2.5 text-sm font-bold text-gray-900">
            기술 스택 <span className="font-medium text-gray-400">{project.techTags.length}</span>
          </h2>
          {project.techTags.length ? (
            <div className="flex flex-wrap gap-1.5">
              {project.techTags.map((tag) => (
                <Badge key={tag.id}>{tag.displayName}</Badge>
              ))}
            </div>
          ) : (
            <p className="text-sm text-gray-500">등록된 기술 스택이 없습니다.</p>
          )}
        </section>
      </aside>
    </div>
  );
}

function ProjectMemberProfile({ member }: { member: ProjectDetail['members'][number] }) {
  const profile = (
    <>
      <Avatar src={member.avatarUrl} name={member.displayName} alt="" size="md" />
      <div className="min-w-0 text-sm">
        <p className="flex min-w-0 flex-wrap items-baseline gap-x-1.5 font-semibold break-words">
          <span>{member.displayName}</span>
          <UserHandle handle={member.handle} userType={member.userType} />
        </p>
        <UserAffiliation {...member} />
      </div>
    </>
  );
  const className =
    'flex items-center gap-3 rounded-sm py-2 focus-visible:outline-primary-600 focus-visible:outline-2';

  return (
    <li className="min-w-0 border-b border-gray-100 last:border-b-0">
      {member.handle ? (
        <Link
          to="/users/$handle"
          params={{ handle: member.handle }}
          aria-label={`${member.displayName} 프로필 보기`}
          className={`${className} group`}
        >
          {profile}
        </Link>
      ) : (
        <div className={className}>{profile}</div>
      )}
    </li>
  );
}
