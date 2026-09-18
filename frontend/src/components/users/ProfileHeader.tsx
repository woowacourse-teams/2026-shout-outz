import { Avatar } from '@/components/Avatar';
import { Badge } from '@/components/Badge';
import { formatCrewRole } from '@/utils/user';

/**
 * 프로필 상단. 아바타 자리, 이름, 소속 배지, 소개, GitHub·블로그 링크를 보여준다.
 *
 * 아바타 이미지는 아직 붙이지 않고 빈 원으로 둔다(응답에 avatarImageId만 온다).
 * 디자인의 "프로필 수정" 버튼은 본인 프로필에서만 필요해 이번 공개 프로필에는 두지 않는다.
 */
export interface ProfileHeaderProps {
  displayName: string;
  cohort: number | null;
  track: string | null;
  bio: string | null;
  githubProfileUrl: string | null;
  blogUrl: string | null;
}

const LINK_STYLE =
  'focus-visible:outline-primary-600 rounded-sm text-sm text-gray-600 underline hover:text-gray-900 focus-visible:outline-2';

export function ProfileHeader({
  displayName,
  cohort,
  track,
  bio,
  githubProfileUrl,
  blogUrl,
}: ProfileHeaderProps) {
  const role = formatCrewRole(cohort, track);

  return (
    <header className="flex flex-col gap-4">
      <div className="flex items-center gap-4">
        {/* TODO avatarImageId를 미디어 API로 이미지 URL로 바꿔 src에 연결 */}
        <Avatar size="lg" alt="" />
        <div className="flex min-w-0 flex-col gap-1.5">
          <h1 className="text-xl font-bold break-words text-gray-900 md:text-2xl">{displayName}</h1>
          {role && (
            <Badge tone="primary" className="w-fit">
              {role}
            </Badge>
          )}
        </div>
      </div>

      {bio && <p className="text-sm leading-relaxed break-words text-gray-600">{bio}</p>}

      {(githubProfileUrl || blogUrl) && (
        <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
          {githubProfileUrl && (
            <a href={githubProfileUrl} className={LINK_STYLE}>
              GitHub
            </a>
          )}
          {blogUrl && (
            <a href={blogUrl} className={LINK_STYLE}>
              블로그
            </a>
          )}
        </div>
      )}
    </header>
  );
}
