import { formatTrackLabel } from '@/utils/user';
import type { Feed } from '@/apis/feed';
import { IconUser } from '@tabler/icons-react';
import { Link } from '@tanstack/react-router';
import { Avatar } from '@/components/Avatar';
import type { AvatarSize } from '@/components/Avatar';
import { CrewStatusBadge } from '@/components/users/CrewStatusBadge';
import { formatRelativeTime } from '@/utils/date';

const ANONYMOUS_AVATAR_SIZE: Record<AvatarSize, string> = {
  xs: 'size-5',
  sm: 'size-7',
  md: 'size-8',
  lg: 'size-13',
};

const ANONYMOUS_ICON_SIZE: Record<AvatarSize, string> = {
  xs: 'size-3.5',
  sm: 'size-4',
  md: 'size-5',
  lg: 'size-8',
};

function formatAuthorRole(author: Feed['author']) {
  if (author.userType !== 'WOOWACOURSE_CREW') return null;

  const track = formatTrackLabel(author.track);
  const cohort = author.cohort != null ? `${author.cohort}기` : null;
  return [cohort, track, '크루'].filter(Boolean).join(' ');
}

export function FeedAuthor({
  author,
  createdAt,
  avatarSize = 'md',
  isAnonymous = false,
  profileLink = true,
}: {
  author: Feed['author'];
  createdAt?: string;
  avatarSize?: AvatarSize;
  isAnonymous?: boolean;
  profileLink?: boolean;
}) {
  const role = author.handle ? formatAuthorRole(author) : null;
  const isOwnAnonymous = isAnonymous && author.handle != null;
  const isCompact = avatarSize === 'xs';
  if (!author.handle) {
    return (
      <div className={`flex min-w-0 items-center ${isCompact ? 'gap-1.5' : 'gap-2'}`}>
        <span
          className={`flex shrink-0 items-center justify-center rounded-full bg-gray-100 text-gray-400 ${ANONYMOUS_AVATAR_SIZE[avatarSize]}`}
        >
          <IconUser className={ANONYMOUS_ICON_SIZE[avatarSize]} aria-hidden="true" />
        </span>
        <div className="min-w-0">
          <div className="flex items-center gap-1.5">
            <p
              className={`text-xs font-semibold text-gray-900 ${isCompact ? 'leading-4' : 'leading-5'}`}
            >
              익명
            </p>
            <CrewStatusBadge
              userType={author.userType}
              cohort={author.cohort}
              isCurrent={author.isCurrent}
              size={isCompact ? 'xs' : 'sm'}
            />
          </div>
          {role && <p className="truncate text-xs leading-4 text-gray-500">{role}</p>}
        </div>
      </div>
    );
  }

  const authorDetails = (
    <>
      <Avatar
        size={avatarSize}
        src={author.avatarUrl}
        name={author.displayName ?? undefined}
        alt=""
      />
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-1.5">
          <span
            className={`group-hover:text-primary-600 truncate text-xs font-semibold text-gray-900 ${isCompact ? 'leading-4' : 'leading-5'}`}
          >
            {author.displayName}
          </span>
          <CrewStatusBadge
            userType={author.userType}
            cohort={author.cohort}
            isCurrent={author.isCurrent}
            size={isCompact ? 'xs' : 'sm'}
          />
          {isOwnAnonymous && (
            <span
              className={`bg-primary-50 text-primary-700 rounded-full text-xs font-medium ${isCompact ? 'px-1.5 py-0' : 'px-2 py-0.5'}`}
            >
              익명으로 작성한 글입니다
            </span>
          )}
        </div>
        {(role || createdAt) && (
          <div className="flex min-w-0 items-center gap-1.5 text-xs leading-4 text-gray-500">
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
    </>
  );

  if (!profileLink) {
    return (
      <div className={`flex min-w-0 items-center ${isCompact ? 'gap-1.5' : 'gap-2'}`}>
        {authorDetails}
      </div>
    );
  }

  return (
    <Link
      to="/users/$handle"
      params={{ handle: author.handle }}
      aria-label={`${author.displayName} 프로필 보기`}
      className={`group focus-visible:outline-primary-600 flex min-w-0 items-center rounded-sm focus-visible:outline-2 ${isCompact ? 'gap-1.5' : 'gap-2'}`}
    >
      {authorDetails}
    </Link>
  );
}
