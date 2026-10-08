import { cn } from '@/utils/cn';
import { WoowacourseIcon } from '@/components/users/WoowacourseIcon';

export function UserHandle({
  handle,
  userType,
  className,
}: {
  handle?: string | null;
  userType?: string | null;
  className?: string;
}) {
  if (!handle) return null;
  return (
    <span
      className={cn(
        'inline-flex min-w-0 items-center gap-1 text-xs leading-4 font-normal text-gray-400',
        className,
      )}
    >
      <span className="min-w-0 truncate">@{handle.replace(/^@/, '')}</span>
      <WoowacourseIcon userType={userType} />
    </span>
  );
}
