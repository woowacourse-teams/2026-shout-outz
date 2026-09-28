import { Tab } from '@/components/Tab';
import { REVIEW_STATUS_LABELS } from '@/constants/admin';

export type ReviewStatus = keyof typeof REVIEW_STATUS_LABELS;

const REVIEW_STATUSES = Object.keys(REVIEW_STATUS_LABELS) as ReviewStatus[];

/** 심사 목록의 상태 필터(대기 · 승인 · 반려). */
export function ReviewStatusTab({
  value,
  onChange,
}: {
  value: ReviewStatus;
  onChange: (next: ReviewStatus) => void;
}) {
  return (
    <Tab
      variant="chip"
      size="sm"
      value={value}
      onChange={(next) => onChange(next as ReviewStatus)}
      aria-label="심사 상태"
    >
      {REVIEW_STATUSES.map((status) => (
        <Tab.Item key={status} value={status}>
          {REVIEW_STATUS_LABELS[status]}
        </Tab.Item>
      ))}
    </Tab>
  );
}
