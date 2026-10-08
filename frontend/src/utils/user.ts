const TRACK_LABELS: Record<string, string> = {
  ANDROID: '안드로이드',
  BACKEND: '백엔드',
  FRONTEND: '프론트엔드',
};

export function formatUserAffiliation({
  userType,
  cohort,
  track,
  isCurrent,
  anonymous = false,
}: {
  userType?: string | null;
  cohort?: number | null;
  track?: string | null;
  isCurrent?: boolean | null;
  anonymous?: boolean;
}): string | null {
  if (userType === 'WOOWACOURSE_COACH') return '우아한테크코스 코치';
  if (userType !== 'WOOWACOURSE_CREW') return null;
  if (!anonymous && cohort != null) {
    return ['우아한테크코스', `${cohort}기`, formatTrackLabel(track)].filter(Boolean).join(' ');
  }
  return isCurrent === false ? '우아한테크코스 수료생' : '우아한테크코스 크루';
}

export function formatTrackLabel(track: string | null | undefined): string | null {
  return track == null ? null : (TRACK_LABELS[track] ?? null);
}

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
  const parts = [cohort == null ? null : `${cohort}기`, formatTrackLabel(track)].filter(Boolean);

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
