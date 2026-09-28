import { IconCircleCheckFilled } from '@tabler/icons-react';
import type { UserType } from '@/types/user';

const CURRENT_COHORT = Number(process.env.CURRENT_COHORT);

export function CrewStatusBadge({
  userType,
  cohort,
  size = 'sm',
}: {
  userType?: UserType | null;
  cohort?: number | null;
  size?: 'xs' | 'sm';
}) {
  if (
    userType !== 'WOOWACOURSE_CREW' ||
    cohort == null ||
    !Number.isInteger(CURRENT_COHORT) ||
    CURRENT_COHORT < 1
  ) {
    return null;
  }

  const sizeClass = size === 'xs' ? 'size-3' : 'size-4';

  if (cohort === CURRENT_COHORT) {
    return (
      <IconCircleCheckFilled
        className={`${sizeClass} text-primary-500 shrink-0`}
        role="img"
        aria-label="우테코 크루"
      />
    );
  }

  if (cohort < CURRENT_COHORT) {
    return (
      <IconCircleCheckFilled
        className={`${sizeClass} shrink-0 text-green-500`}
        role="img"
        aria-label="우테코 수료 크루"
      />
    );
  }

  return null;
}
