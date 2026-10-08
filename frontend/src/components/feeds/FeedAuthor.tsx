import type { Feed } from '@/apis/feed';
import { Link } from '@tanstack/react-router';
import { Avatar } from '@/components/Avatar';
import type { AvatarSize } from '@/components/Avatar';
import { UserAffiliation } from '@/components/users/UserAffiliation';
import { WoowacourseIcon } from '@/components/users/WoowacourseIcon';
import { UserHandle } from '@/components/users/UserHandle';
import { formatRelativeTime } from '@/utils/date';

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
  const isOwnAnonymous = isAnonymous && author.handle != null;
  const isCompact = avatarSize === 'xs';
  const displayAvatarSize = avatarSize;
  if (!author.handle) {
    return (
      <div className={`flex min-w-0 items-center ${isCompact ? 'gap-1.5' : 'gap-2'}`}>
        <Avatar size={displayAvatarSize} anonymous alt="" />
        <div className="min-w-0">
          <div className="flex items-center gap-1.5">
            <p
              className={`text-xs font-semibold text-gray-900 ${isCompact ? 'leading-4' : 'leading-5'}`}
            >
              익명
            </p>
            <WoowacourseIcon userType={author.userType} />
          </div>
          <UserAffiliation {...author} anonymous className={isCompact ? 'leading-4' : undefined} />
        </div>
      </div>
    );
  }

  const authorDetails = (
    <>
      <Avatar
        size={displayAvatarSize}
        src={author.avatarUrl}
        name={author.displayName ?? undefined}
        alt=""
      />
      <div className="min-w-0">
        <div className="flex flex-wrap items-baseline gap-1.5">
          <span
            className={`group-hover:text-primary-600 truncate text-xs font-semibold text-gray-900 ${isCompact ? 'leading-4' : 'leading-5'}`}
          >
            {author.displayName}
          </span>
          <UserHandle handle={author.handle} userType={author.userType} />
          {isOwnAnonymous && (
            <span
              className={`bg-primary-50 text-primary-700 rounded-full text-xs font-medium ${isCompact ? 'px-1.5 py-0' : 'px-2 py-0.5'}`}
            >
              익명으로 작성한 글입니다
            </span>
          )}
        </div>
        <UserAffiliation {...author} className={isCompact ? 'leading-4' : undefined} />
        {createdAt && (
          <time className="block text-xs leading-4 text-gray-400" dateTime={createdAt}>
            {formatRelativeTime(createdAt)}
          </time>
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
