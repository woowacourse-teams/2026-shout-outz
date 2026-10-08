import { IconCircleCheckFilled } from '@tabler/icons-react';

export function CrewStatusBadge({
  userType,
  size = 'sm',
}: {
  /** 인증된 크루와 코치를 같은 색상으로 표시한다. */
  userType?: string | null;
  size?: 'xs' | 'sm';
}) {
  if (userType !== 'WOOWACOURSE_CREW' && userType !== 'WOOWACOURSE_COACH') return null;

  return (
    <IconCircleCheckFilled
      className={`${size === 'xs' ? 'mt-1 size-3' : 'size-4'} shrink-0 text-green-500`}
      role="img"
      aria-label="우아한테크코스 소속 인증"
    />
  );
}
