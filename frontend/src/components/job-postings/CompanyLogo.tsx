import { Image } from '@/components/Image';
import { cn } from '@/utils/cn';

/**
 * 채용공고 회사 로고. 로고가 없거나 못 불러오면 같은 크기의 회색 자리를 둔다.
 * 크기와 모서리는 쓰는 곳에서 className으로 정한다.
 */
export function CompanyLogo({
  companyName,
  logoUrl,
  decorative = false,
  className,
}: {
  companyName: string;
  logoUrl: string | null | undefined;
  /** 바로 옆에 회사 이름이 있어 읽어 줄 필요가 없으면 true */
  decorative?: boolean;
  className?: string;
}) {
  const label = decorative ? undefined : `${companyName} 로고`;
  return (
    <Image
      src={logoUrl ?? undefined}
      alt={label ?? ''}
      // 로고는 잘리지 않게 칸 안에 맞추고 흰 바탕에 둔다.
      className={cn('shrink-0 border border-gray-100 bg-white object-contain p-1', className)}
      fallback={
        <div
          {...(label ? { role: 'img', 'aria-label': label } : { 'aria-hidden': true })}
          className={cn('shrink-0 bg-gray-100', className)}
        />
      }
    />
  );
}
