import { CrewStatusBadge } from '@/components/users/CrewStatusBadge';
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
  const label = formatUserAffiliation(user);
  if (!label) return null;

  return (
    <span
      className={cn('flex min-w-0 items-start gap-1 text-xs leading-5 text-gray-500', className)}
    >
      <CrewStatusBadge userType={user.userType} size="xs" />
      <span className="min-w-0 break-keep whitespace-normal">{label}</span>
    </span>
  );
}
