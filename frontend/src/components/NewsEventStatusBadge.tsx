import { Badge, type BadgeTone, type BadgeVariant } from '@/components/Badge';
import type { NewsEventStatus } from '@/types/news';

const STATUS: Record<NewsEventStatus, { label: string; tone: BadgeTone }> = {
  UPCOMING: { label: '예정', tone: 'primary' },
  ONGOING: { label: '진행 중', tone: 'green' },
  ENDED: { label: '종료', tone: 'gray' },
};

export function NewsEventStatusBadge({
  status,
  variant = 'solid',
}: {
  status?: NewsEventStatus | null;
  /** 새 디자인 소식 화면은 옅은 배지(soft)를 쓴다. */
  variant?: BadgeVariant;
}) {
  if (!status) return null;
  const { label, tone } = STATUS[status];
  return (
    <Badge variant={variant} tone={tone}>
      {label}
    </Badge>
  );
}
