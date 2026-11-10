import { infiniteQueryOptions, queryOptions } from '@tanstack/react-query';

import type { Material } from '@/apis/explore';
import { PROJECT_PAGE_SIZE } from '@/constants/project';
import type { ProjectListMeta, ProjectSort, ProjectSummary } from '@/types/project';
import { httpClient, type ApiSuccessBody } from '@/utils/client';

// MOCK-API: 기술 주제 필터, 프로젝트 경험, 프로젝트 자료는 서버 API가 없어 화면에 필요한 모양대로 임의로 정했다.
// 프로젝트 목록은 서버의 GET /api/v1/projects를 쓰고, 서버에 없는 topic 조건만 덧붙인다.

/** 기술 주제를 묶는 직군. 공통은 여러 직군이 함께 다루는 주제다. */
export type ProjectTopicGroupCode = 'COMMON' | 'BE' | 'FE' | 'AN';

export interface ProjectTopic {
  name: string;
  /** 이 주제를 다룬 프로젝트 수 */
  projectCount: number;
}

export interface ProjectTopicGroup {
  code: ProjectTopicGroupCode;
  /** 예: 백엔드 */
  name: string;
  /** 예: BE */
  shortName: string;
  projectCount: number;
  /** 프로젝트가 많은 순 */
  topics: ProjectTopic[];
}

export interface ProjectTopics {
  /** 주제가 하나라도 있는 프로젝트 수 */
  totalCount: number;
  groups: ProjectTopicGroup[];
}

export interface ExploreProjectFilter {
  keyword: string;
  /** 기술 주제 이름. 없으면 모든 주제 */
  topic: string | null;
  sort: ProjectSort;
}

export type ExperienceJobRole = 'BE' | 'FE' | 'AN';
export type ExperienceSourceKind = 'PR' | 'TEAM_DOC' | 'BLOG';

export interface ProjectExperienceSource {
  kind: ExperienceSourceKind;
  title: string;
  url: string;
  /** PR 번호. PR이 아니면 null */
  number: number | null;
}

/** 프로젝트에서 한 경험. 팀 자료에서 뽑아 요약한 것이다. */
export interface ProjectExperience {
  experienceId: number;
  jobRole: ExperienceJobRole;
  /** 예: 장애 대응, 쿼리 최적화 */
  category: string;
  title: string;
  /** 무엇을 했는지 한 줄씩 */
  highlights: string[];
  sources: ProjectExperienceSource[];
}

export type MaterialJobRole = 'ALL' | ExperienceJobRole;

export interface ProjectMaterialTopics {
  /** 이 프로젝트 자료 수 */
  totalCount: number;
  kindCounts: { blog: number; pr: number; teamDoc: number };
  jobRoles: { jobRole: MaterialJobRole; count: number }[];
  /** 고른 직군의 자료를 분야별 주제로 묶은 것. 자료가 많은 순 */
  fields: { name: string; topics: { name: string; count: number }[] }[];
}

export interface ProjectTopicMaterials {
  topic: string;
  /** 이 프로젝트 팀의 자료 */
  materials: Material[];
  techTalks: Material[];
  /** 같은 주제를 다룬 다른 팀의 자료 */
  otherTeamMaterials: Material[];
}

const projectPath = (slug: string) => `/api/v1/projects/@${slug}`;

export async function fetchExploreProjectsPage(
  { keyword, topic, sort }: ExploreProjectFilter,
  cursor?: string,
  signal?: AbortSignal,
) {
  const searchParams: Record<string, string | number> = { sort, size: PROJECT_PAGE_SIZE };
  if (keyword.trim()) searchParams.keyword = keyword.trim();
  if (topic) searchParams.topic = topic;
  if (cursor) searchParams.cursor = cursor;

  const response = await httpClient<ApiSuccessBody<ProjectSummary[], ProjectListMeta>>(
    '/api/v1/projects',
    { method: 'get', signal, searchParams },
  );
  if (!response || !Array.isArray(response.data))
    throw new Error('프로젝트 목록 응답이 비어 있습니다.');
  return {
    projects: response.data,
    meta: response.meta ?? { nextCursor: null, hasNext: false, totalCount: response.data.length },
  };
}

async function get<T>(path: string, signal?: AbortSignal, searchParams?: Record<string, string>) {
  const response = await httpClient<ApiSuccessBody<T>>(path, {
    method: 'get',
    signal,
    searchParams,
  });
  if (!response?.data) throw new Error(`응답이 비어 있습니다: ${path}`);
  return response.data;
}

export const exploreProjectsQuery = (filter: ExploreProjectFilter) =>
  infiniteQueryOptions({
    queryKey: ['explore', 'projects', filter],
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam, signal }) => fetchExploreProjectsPage(filter, pageParam, signal),
    getNextPageParam: (last) =>
      last.meta.hasNext && last.meta.nextCursor ? last.meta.nextCursor : undefined,
  });

export const projectTopicsQuery = () =>
  queryOptions({
    queryKey: ['explore', 'project-topics'],
    queryFn: ({ signal }) => get<ProjectTopics>('/api/v1/projects/topics', signal),
  });

export const projectExperiencesQuery = (slug: string) =>
  queryOptions({
    queryKey: ['project-detail', slug, 'experiences'],
    queryFn: ({ signal }) => get<ProjectExperience[]>(`${projectPath(slug)}/experiences`, signal),
  });

export const projectMaterialTopicsQuery = (slug: string, jobRole: MaterialJobRole) =>
  queryOptions({
    queryKey: ['project-detail', slug, 'material-topics', jobRole],
    queryFn: ({ signal }) =>
      get<ProjectMaterialTopics>(`${projectPath(slug)}/material-topics`, signal, { jobRole }),
  });

export const projectTopicMaterialsQuery = (slug: string, topic: string) =>
  queryOptions({
    queryKey: ['project-detail', slug, 'materials', topic],
    queryFn: ({ signal }) =>
      get<ProjectTopicMaterials>(`${projectPath(slug)}/materials`, signal, { topic }),
  });
