// TODO 서버의 트랙 허용 값이 확정되면 대체
const TRACK_LABELS: Record<string, string> = {
  BACKEND: '백엔드',
  FRONTEND: '프론트엔드',
};

/**
 * 크루의 소속 표시 문구.
 *
 * - 기수와 트랙을 모두 알면: "6기 백엔드"
 * - 아는 것만 있으면 그것만: "6기", "백엔드"
 * - 둘 다 없거나 한글 표기를 모르는 트랙뿐이면: null (표시하지 않는다)
 *
 * 프로필 배지와 피드 작성자 줄이 같은 규칙을 쓴다.
 */
export function formatCrewRole(
  cohort: number | null | undefined,
  track: string | null | undefined,
): string | null {
  const parts = [
    cohort == null ? null : `${cohort}기`,
    track == null ? null : TRACK_LABELS[track],
  ].filter(Boolean);

  return parts.length === 0 ? null : parts.join(' ');
}

/** 이름과 소속을 이어 붙인다. 소속을 알 수 없으면 이름만 남는다. */
export function formatCrewName(
  displayName: string,
  cohort: number | null | undefined,
  track: string | null | undefined,
): string {
  return [displayName, formatCrewRole(cohort, track)].filter(Boolean).join(' · ');
}
