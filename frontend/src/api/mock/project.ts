import { type CohortOption, type CrewSearchItem, type TechTag } from '@/types/project';

/** 실제 서버가 준비되기 전까지 MSW 핸들러가 내려줄 프로젝트 등록용 선택지. 백엔드가 뜨면 이 파일은 사라진다. */
const COHORTS: CohortOption[] = [
  { cohort: 8, year: 2026 },
  { cohort: 7, year: 2025 },
  { cohort: 6, year: 2024 },
];

const TECH_TAGS: TechTag[] = [
  { id: 1, displayName: 'React' },
  { id: 2, displayName: 'TypeScript' },
  { id: 3, displayName: 'Spring Boot' },
  { id: 4, displayName: 'Redis' },
];

const crew = (
  handle: string,
  displayName: string,
  track: string,
  cohort: number,
): CrewSearchItem => ({
  handle,
  displayName,
  userType: 'WOOWACOURSE_CREW',
  track,
  cohort,
  avatarImageId: null,
});

const CREWS: CrewSearchItem[] = [
  crew('zzaekkii', '재키', 'BACKEND', 8),
  crew('dhyepark', '두리', 'FRONTEND', 8),
  crew('hoik', '황호익', 'BACKEND', 6),
];

export function getCohorts(): CohortOption[] {
  return COHORTS;
}

/** keyword를 생략하면 전체 태그. 대소문자 무시 부분 일치 */
export function getTechTags(keyword?: string | null): TechTag[] {
  if (!keyword) return TECH_TAGS;

  const needle = keyword.toLowerCase();
  return TECH_TAGS.filter((tag) => tag.displayName.toLowerCase().includes(needle));
}

/** 이름(닉네임) 또는 handle 부분 일치 */
export function searchCrewList(keyword: string): CrewSearchItem[] {
  const needle = keyword.trim().toLowerCase();
  if (!needle) return [];

  return CREWS.filter(
    (item) =>
      item.displayName.toLowerCase().includes(needle) || item.handle.toLowerCase().includes(needle),
  );
}
