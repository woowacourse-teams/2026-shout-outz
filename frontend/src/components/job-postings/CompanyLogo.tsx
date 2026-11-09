import { cn } from '@/utils/cn';

interface CompanyLogoProps {
  companyName: string;
  logoUrl: string | null;
  className?: string;
}

/** 회사 로고. 로고가 없으면 회색 사각형으로 자리를 잡는다. */
export function CompanyLogo({ companyName, logoUrl, className }: CompanyLogoProps) {
  if (logoUrl)
    return (
      <img
        src={logoUrl}
        alt={`${companyName} 로고`}
        className={cn('shrink-0 border border-gray-100 object-contain', className)}
      />
    );

  return (
    <span
      role="img"
      aria-label={`${companyName} 로고`}
      className={cn('ring-gray-opacity-100 shrink-0 bg-gray-200 ring-1 ring-inset', className)}
    />
  );
}
