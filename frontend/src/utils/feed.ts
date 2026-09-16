import { type FeedAuthor } from '@/types/feed';

const TRACK_LABELS: Record<string, string> = {
  BACKEND: '백엔드',
  FRONTEND: '프론트엔드',
};

/**
 * 피드 카드 작성자 줄에 쓰는 표시 문구.
 *
 * - 기수와 트랙이 모두 있으면: "황호익 · 6기 백엔드"
 * - 둘 중 하나라도 없거나 한글 표기를 모르는 트랙이면: "황호익"
 */
export function formatAuthorLabel({ displayName, track, cohort }: FeedAuthor): string {
  // TODO 서버의 트랙 허용 값이 확정되면 TRACK_LABELS 대체
  const trackLabel = track === null ? undefined : TRACK_LABELS[track];
  if (trackLabel === undefined || cohort === null) return displayName;

  return `${displayName} · ${cohort}기 ${trackLabel}`;
}
