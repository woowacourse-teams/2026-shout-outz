import { IconCircleCheckFilled } from '@tabler/icons-react';

const CURRENT_COHORT = Number(process.env.CURRENT_COHORT);

export function CrewStatusBadge({
  userType,
  cohort,
  isCurrent,
  size = 'sm',
}: {
  /** 응답마다 문자열이나 열거값으로 달라 넓게 받는다. 크루 여부만 비교한다. */
  userType?: string | null;
  cohort?: number | null;
  /** 피드·댓글 작성자 응답은 기수 번호 대신 현재 기수 여부를 제공한다. */
  isCurrent?: boolean | null;
  size?: 'xs' | 'sm';
}) {
  if (userType !== 'WOOWACOURSE_CREW') return null;

  const current =
    isCurrent ??
    (cohort != null && Number.isInteger(CURRENT_COHORT) && CURRENT_COHORT > 0
      ? cohort <= CURRENT_COHORT
        ? cohort === CURRENT_COHORT
        : null
      : null);
  if (current == null) return null;

  const sizeClass = size === 'xs' ? 'size-3' : 'size-4';

  if (current) {
    return (
      <IconCircleCheckFilled
        className={`${sizeClass} text-primary-500 shrink-0`}
        role="img"
        aria-label="우테코 크루"
      />
    );
  }

  return (
    <IconCircleCheckFilled
      className={`${sizeClass} shrink-0 text-green-500`}
      role="img"
      aria-label="우테코 수료 크루"
    />
  );
}
