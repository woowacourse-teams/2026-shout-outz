import { cn } from '@/utils/cn';

/** 작은 크기에서도 식별되는 행성과 궤도 모티프의 소속 아이콘. */
export function WoowacourseIcon({
  userType,
  className,
}: {
  userType?: string | null;
  className?: string;
}) {
  if (userType !== 'WOOWACOURSE_CREW' && userType !== 'WOOWACOURSE_COACH') return null;

  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      role="img"
      aria-label="우아한테크코스 소속"
      className={cn('inline-block size-4 shrink-0 align-text-bottom text-gray-600', className)}
    >
      <title>우아한테크코스 소속</title>
      <path d="M5.2 12.7a7 7 0 0 1 12.9-5.1M18.8 11.3a7 7 0 0 1-12.9 5.1" />
      <path
        d="M6 9.4C2.8 11.7 1.4 14 2.2 15.2c1.1 1.8 6.4.5 11.8-2.8s8.8-7.4 7.7-9.2c-.8-1.2-3.4-.9-6.5.4"
        transform="translate(0 2)"
      />
      <circle cx="10" cy="8" r=".8" fill="currentColor" stroke="none" />
      <circle cx="14" cy="7" r=".6" fill="currentColor" stroke="none" />
    </svg>
  );
}
