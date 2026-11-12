import { cn } from '@/utils/cn';
import { WoowacourseIcon } from '@/components/users/WoowacourseIcon';

export function UserHandle({
  handle,
  userType,
  className,
  badgeFocusable = true,
}: {
  handle?: string | null;
  userType?: string | null;
  className?: string;
  /** 소속 인증 아이콘이 키보드 포커스를 받을지 정합니다. */
  badgeFocusable?: boolean;
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
      <WoowacourseIcon userType={userType} focusable={badgeFocusable} />
    </span>
  );
}
