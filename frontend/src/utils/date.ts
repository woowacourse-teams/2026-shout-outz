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
