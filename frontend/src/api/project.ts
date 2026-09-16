import { mutationOptions, queryOptions } from '@tanstack/react-query';
import {
  type CohortOption,
  type CrewSearchItem,
  type ProjectCreated,
  type ProjectCreateRequest,
  type TechTag,
} from '@/types/project';

export async function createProject(request: ProjectCreateRequest): Promise<ProjectCreated> {
  throw new Error('createProject는 아직 구현되지 않았습니다.');
}

export const createProjectMutationOptions = mutationOptions({
  mutationFn: createProject,
});

export async function fetchCohorts(): Promise<CohortOption[]> {
  throw new Error('fetchCohorts는 아직 구현되지 않았습니다.');
}

export const cohortsQueryOptions = () =>
  queryOptions({
    queryKey: ['cohorts'],
    queryFn: fetchCohorts,
  });

/** keyword를 생략하면 전체 태그를 돌려준다 */
export async function fetchTechTags(keyword?: string): Promise<TechTag[]> {
  throw new Error('fetchTechTags는 아직 구현되지 않았습니다.');
}

export const techTagsQueryOptions = (keyword?: string) =>
  queryOptions({
    queryKey: ['tech-tags', keyword],
    queryFn: () => fetchTechTags(keyword),
  });

/** 참여 팀원으로 추가할 크루를 이름 또는 handle로 검색한다 */
export async function searchCrews(keyword: string): Promise<CrewSearchItem[]> {
  throw new Error('searchCrews는 아직 구현되지 않았습니다.');
}

export const crewSearchQueryOptions = (keyword: string) =>
  queryOptions({
    queryKey: ['crew-search', keyword],
    queryFn: () => searchCrews(keyword),
  });
