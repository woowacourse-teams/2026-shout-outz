import type { Feed } from '@/apis/feed';
import { Link } from '@tanstack/react-router';
import { Avatar } from '@/components/Avatar';
import type { AvatarSize } from '@/components/Avatar';
import { CrewStatusBadge } from '@/components/users/CrewStatusBadge';
import { formatRelativeTime } from '@/utils/date';

const TRACK_ABBREVIATIONS: Record<string, string> = {
  BACKEND: 'BE',
  FRONTEND: 'FE',
  ANDROID: 'AOS',
};

function formatAuthorRole(author: Feed['author']) {
  if (author.userType !== 'WOOWACOURSE_CREW') return null;

  const track = author.track ? TRACK_ABBREVIATIONS[author.track] : null;
  return [track, author.cohort == null ? null : `${author.cohort}기`, '크루']
    .filter(Boolean)
    .join(' ');
}

export function FeedAuthor({
  author,
  createdAt,
  avatarSize = 'md',
}: {
  author: Feed['author'];
  createdAt?: string;
  avatarSize?: AvatarSize;
}) {
  const role = formatAuthorRole(author);

  return (
    <Link
      to="/users/$handle"
      params={{ handle: author.handle }}
      aria-label={`${author.displayName} 프로필 보기`}
      className="group focus-visible:outline-primary-600 flex min-w-0 items-center gap-2 rounded-sm focus-visible:outline-2"
    >
      {/* 이름이 바로 옆에 있으므로 아바타는 장식이다. 사진이 없으면 이름 첫 글자로 그린다. */}
      <Avatar size={avatarSize} src={author.avatarUrl} name={author.displayName} alt="" />
      <div className="min-w-0">
        <div className="flex items-center gap-1.5">
          <span className="group-hover:text-primary-600 truncate text-sm font-semibold text-gray-900">
            {author.displayName}
          </span>
          <CrewStatusBadge userType={author.userType} cohort={author.cohort} />
        </div>
        {(role || createdAt) && (
          <div className="flex min-w-0 items-center gap-1.5 text-sm text-gray-500">
            {role && <span className="truncate">{role}</span>}
            {createdAt && (
              <>
                {role && <span aria-hidden="true">·</span>}
                <time className="shrink-0 text-gray-400" dateTime={createdAt}>
                  {formatRelativeTime(createdAt)}
                </time>
              </>
            )}
          </div>
        )}
      </div>
    </Link>
  );
}
