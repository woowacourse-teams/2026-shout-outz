const ISO_DATE = /^\d{4}-\d{2}-\d{2}/;

const KST_DATE = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});

export function formatDotDate(isoDateTime: string): string {
  if (!ISO_DATE.test(isoDateTime)) {
    throw new Error(`ISO 8601 날짜로 시작하지 않습니다: ${isoDateTime}`);
  }

  return KST_DATE.formatToParts(new Date(isoDateTime))
    .filter((part) => part.type !== 'literal')
    .map((part) => part.value)
    .join('.');
}

// TODO 정책 논의 필요
/**
 * 작성 시각을 현재 시각 기준 상대 표현으로 바꾼다.
 * - 1분 미만: "방금 전"
 * - 1시간 미만: "N분 전"
 * - 1일 미만: "N시간 전"
 * - 7일 미만: "N일 전"
 * - 그 이상: `formatDotDate` 결과
 */
export function formatRelativeTime(isoDateTime: string, now: Date = new Date()): string {
  throw new Error('formatRelativeTime은 아직 구현되지 않았습니다.');
}
