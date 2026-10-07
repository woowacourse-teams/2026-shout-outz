import { Badge, type BadgeTone } from '@/components/Badge';
import type { NewsEventStatus } from '@/types/news';

const STATUS: Record<NewsEventStatus, { label: string; tone: BadgeTone }> = {
  UPCOMING: { label: '예정', tone: 'primary' },
  ONGOING: { label: '진행 중', tone: 'green' },
  ENDED: { label: '종료', tone: 'gray' },
};

export function NewsEventStatusBadge({ status }: { status?: NewsEventStatus | null }) {
  if (!status) return null;
  const { label, tone } = STATUS[status];
  return (
    <Badge variant="solid" tone={tone}>
      {label}
    </Badge>
  );
}
