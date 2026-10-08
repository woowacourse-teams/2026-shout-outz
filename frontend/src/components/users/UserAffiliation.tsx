import { formatUserAffiliation } from '@/utils/user';
import { cn } from '@/utils/cn';

export interface UserAffiliationProps {
  userType?: string | null;
  cohort?: number | null;
  track?: string | null;
  isCurrent?: boolean | null;
  anonymous?: boolean;
  className?: string;
}

export function UserAffiliation({ className, ...user }: UserAffiliationProps) {
  const label = formatUserAffiliation(user)?.replace(/^우아한테크코스\s+/, '');
  if (!label) return null;

  return (
    <span
      className={cn(
        'block min-w-0 text-xs leading-5 break-keep whitespace-normal text-gray-500',
        className,
      )}
    >
      {label}
    </span>
  );
}
