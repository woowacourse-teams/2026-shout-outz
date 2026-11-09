import { cn } from '@/utils/cn';

// ASSUMPTION: API에 회사 로고가 없어 프로토타입의 자리 표시처럼 회색 사각형을 둔다. 로고가 생기면 img로 바꾼다.
export function CompanyLogo({
  companyName,
  className,
}: {
  companyName: string;
  className?: string;
}) {
  return (
    <span
      role="img"
      aria-label={`${companyName} 로고`}
      className={cn('ring-gray-opacity-100 shrink-0 bg-gray-200 ring-1 ring-inset', className)}
    />
  );
}
