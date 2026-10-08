import type { ReactNode } from 'react';
import { IconBrandGithub, IconWorld } from '@tabler/icons-react';

import { Avatar } from '@/components/Avatar';
import { CrewStatusBadge } from '@/components/users/CrewStatusBadge';
import type { UserType } from '@/types/user';
import { formatCrewRole } from '@/utils/user';

/**
 * 프로필 상단. 아바타 자리, 이름, 소속 배지, 소개, GitHub·블로그 링크를 보여준다.
 *
 * 선택 필드는 서버가 값을 아예 빼고 주기도 해서 undefined까지 받는다.
 */
export interface ProfileHeaderProps {
  displayName: string;
  userType?: UserType;
  cohort?: number | null;
  track?: string | null;
  bio?: string | null;
  githubProfileUrl?: string | null;
  blogUrl?: string | null;
  avatarUrl?: string | null;
  actions?: ReactNode;
}

const LINK_STYLE =
  'focus-visible:outline-primary-600 inline-flex items-center gap-1 rounded-sm text-sm text-gray-600 hover:text-gray-900 hover:underline focus-visible:outline-2';

export function ProfileHeader({
  displayName,
  userType,
  cohort,
  track,
  bio,
  githubProfileUrl,
  blogUrl,
  avatarUrl,
  actions,
}: ProfileHeaderProps) {
  const role = formatCrewRole(cohort, track);

  return (
    <header className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-4">
        <div className="flex min-w-0 items-center gap-4">
          <Avatar size="lg" src={avatarUrl} name={displayName} alt="" />
          <div className="flex min-w-0 flex-col gap-1.5">
            <div className="flex items-center gap-1.5">
              <h1 className="text-xl font-bold break-words text-gray-900 md:text-2xl">
                {displayName}
              </h1>
              {userType && <CrewStatusBadge userType={userType} cohort={cohort} />}
            </div>
            {role && <p className="text-sm text-gray-500">{role}</p>}
          </div>
        </div>
        {actions}
      </div>

      {bio && <p className="text-sm leading-relaxed break-words text-gray-600">{bio}</p>}

      {(githubProfileUrl || blogUrl) && (
        <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
          {githubProfileUrl && (
            <a
              href={githubProfileUrl}
              target="_blank"
              rel="noopener noreferrer"
              className={LINK_STYLE}
            >
              <IconBrandGithub className="size-4" aria-hidden="true" />
              GitHub
            </a>
          )}
          {blogUrl && (
            <a href={blogUrl} target="_blank" rel="noopener noreferrer" className={LINK_STYLE}>
              <IconWorld className="size-4" aria-hidden="true" />
              블로그
            </a>
          )}
        </div>
      )}
    </header>
  );
}
