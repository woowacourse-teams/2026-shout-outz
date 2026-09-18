import { mutationOptions, queryOptions } from '@tanstack/react-query';
import {
  type CohortOption,
  type CrewSearchItem,
  type ProjectCreated,
  type ProjectCreateRequest,
  type TechTag,
} from '@/types/project';
import { type ApiSuccessBody, httpClient } from '@/utils/client';

const PROJECTS_PATH = '/api/v1/projects';
const COHORTS_PATH = '/api/v1/cohorts';
const TECH_TAGS_PATH = '/api/v1/tech-tags';
const USER_SEARCH_PATH = '/api/v1/users/search';

interface Items<T> {
  items: T[];
}

export async function createProject(request: ProjectCreateRequest): Promise<ProjectCreated> {
  const body = await httpClient<ApiSuccessBody<ProjectCreated>>(PROJECTS_PATH, {
    method: 'post',
    json: request,
  });
  if (!body) throw new Error(`프로젝트 등록 응답이 비어 있습니다: ${PROJECTS_PATH}`);

  return body.data;
}

export const createProjectMutationOptions = mutationOptions({
  mutationFn: createProject,
});

export async function fetchCohorts(): Promise<CohortOption[]> {
  const body = await httpClient<ApiSuccessBody<Items<CohortOption>>>(COHORTS_PATH, {
    method: 'get',
  });
  if (!body) throw new Error(`기수 목록 응답이 비어 있습니다: ${COHORTS_PATH}`);

  return body.data.items;
}

export const cohortsQueryOptions = () =>
  queryOptions({
    queryKey: ['cohorts'],
    queryFn: fetchCohorts,
  });

/** keyword를 생략하면 전체 태그를 돌려준다 */
export async function fetchTechTags(keyword?: string): Promise<TechTag[]> {
  const body = await httpClient<ApiSuccessBody<Items<TechTag>>>(TECH_TAGS_PATH, {
    method: 'get',
    searchParams: { keyword },
  });
  if (!body) throw new Error(`기술 스택 응답이 비어 있습니다: ${TECH_TAGS_PATH}`);

  return body.data.items;
}

export const techTagsQueryOptions = (keyword?: string) =>
  queryOptions({
    queryKey: ['tech-tags', keyword],
    queryFn: () => fetchTechTags(keyword),
  });

/** 참여 팀원으로 추가할 크루를 이름 또는 handle로 검색한다 */
export async function searchCrews(keyword: string): Promise<CrewSearchItem[]> {
  const body = await httpClient<ApiSuccessBody<Items<CrewSearchItem>>>(USER_SEARCH_PATH, {
    method: 'get',
    searchParams: { keyword },
  });
  if (!body) throw new Error(`크루 검색 응답이 비어 있습니다: ${USER_SEARCH_PATH}`);

  return body.data.items;
}

export const crewSearchQueryOptions = (keyword: string) =>
  queryOptions({
    queryKey: ['crew-search', keyword],
    queryFn: () => searchCrews(keyword),
  });
