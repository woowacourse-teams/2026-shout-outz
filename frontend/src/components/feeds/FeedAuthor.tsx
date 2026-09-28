import type { Feed } from '@/apis/feed';
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
    <div className="flex min-w-0 items-center gap-2">
      <Avatar size={avatarSize} src={author.avatarUrl ?? undefined} alt="" />
      <div className="min-w-0">
        <div className="flex items-center gap-1.5">
          <span className="truncate text-sm font-semibold text-gray-900">{author.displayName}</span>
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
    </div>
  );
}
