const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})/;

export function formatDotDate(isoDateTime: string): string {
  const matched = ISO_DATE.exec(isoDateTime);

  if (!matched) {
    throw new Error(`ISO 8601 날짜로 시작하지 않습니다: ${isoDateTime}`);
  }

  return matched.slice(1).join('.');
}
