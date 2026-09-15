const ISO_DATE = /^\d{4}-\d{2}-\d{2}/;

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

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
  if (!ISO_DATE.test(isoDateTime)) {
    throw new Error(`ISO 8601 날짜로 시작하지 않습니다: ${isoDateTime}`);
  }

  // 서버와 기기의 시계 차이로 미래 시각이 오면 음수가 되어 "방금 전"으로 떨어진다.
  const elapsed = now.getTime() - new Date(isoDateTime).getTime();

  if (elapsed < MINUTE) return '방금 전';
  if (elapsed < HOUR) return `${Math.floor(elapsed / MINUTE)}분 전`;
  if (elapsed < DAY) return `${Math.floor(elapsed / HOUR)}시간 전`;
  if (elapsed < 7 * DAY) return `${Math.floor(elapsed / DAY)}일 전`;

  return formatDotDate(isoDateTime);
}
