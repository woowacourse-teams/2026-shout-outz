/**
 * 크루의 소속 표시 문구.
 *
 * - 기수와 트랙이 모두 있고 트랙의 한글 표기를 알면: "6기 백엔드"
 * - 그 밖에는 null (표시하지 않는다)
 *
 * 프로필 배지와 피드 작성자 줄(`formatAuthorLabel`)이 같은 규칙을 쓴다.
 */
export function formatCrewRole(cohort: number | null, track: string | null): string | null {
  throw new Error('formatCrewRole은 아직 구현되지 않았습니다.');
}
