import { infiniteQueryOptions, queryOptions } from '@tanstack/react-query';

import type {
  ProjectCareerCardsSuccessResponse,
  ProjectResourceTopicsSuccessResponse,
  ProjectTopicResourcesSuccessResponse,
  ProjectTopicsSuccessResponse,
} from '@/api/generated/schema';
import { PROJECT_PAGE_SIZE } from '@/constants/project';
import type { ProjectListMeta, ProjectSort, ProjectSummary } from '@/types/project';
import { httpClient, type ApiSuccessBody } from '@/utils/client';

/** 탐색 화면 프로젝트 탭의 기술 주제. 직군(공통·백엔드·프론트엔드·안드로이드)별로 묶여 있다. */
export type ProjectTopics = ProjectTopicsSuccessResponse['data'];
export type ProjectTopicGroup = ProjectTopics['groups'][number];
export type ProjectTopicGroupCode = ProjectTopicGroup['code'];
export type ProjectTopic = ProjectTopicGroup['topics'][number];

export interface ExploreProjectFilter {
  keyword: string;
  /** 기술 주제 이름. 없으면 모든 주제 */
  topic: string | null;
  sort: ProjectSort;
}

type CareerCardResponse = ProjectCareerCardsSuccessResponse['data'][number];

/**
 * 프로젝트 상세 경험 탭의 경력 카드.
 *
 * 스키마는 resumeBullets·techs를 `(object | boolean | string | number)[]`로 뽑는다(생성기가 배열 원소 타입을 못 읽은 결과다).
 * 서버가 주는 값은 문장이라 string[]로 좁혀 쓴다. sections(포트폴리오 글)는 보여 줄 화면 디자인이 아직 없어 쓰지 않는다.
 */
export interface ProjectCareerCard extends Omit<CareerCardResponse, 'resumeBullets' | 'techs'> {
  /** 이력서 문장. 카드에 점 찍힌 줄로 보여 준다. */
  resumeBullets: string[];
  techs: string[];
}

export type CareerCardSource = ProjectCareerCard['sources'][number];

export type ProjectResourceTopics = ProjectResourceTopicsSuccessResponse['data'];
export type MaterialJobRole = ProjectResourceTopics['jobRoles'][number]['jobRole'];

export type ProjectTopicResources = ProjectTopicResourcesSuccessResponse['data'];

const projectPath = (slug: string) => `/api/v1/projects/@${slug}`;

/** 목록 조회 조건. 서버는 기술 주제를 이름이 아니라 topicId로 받는다. */
export interface ExploreProjectQuery {
  keyword: string;
  topicId: string | null;
  sort: ProjectSort;
}

export async function fetchExploreProjectsPage(
  { keyword, topicId, sort }: ExploreProjectQuery,
  cursor?: string,
  signal?: AbortSignal,
) {
  const searchParams: Record<string, string | number> = { sort, size: PROJECT_PAGE_SIZE };
  if (keyword.trim()) searchParams.keyword = keyword.trim();
  if (topicId) searchParams.topicId = topicId;
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

export const exploreProjectsQuery = (filter: ExploreProjectQuery) =>
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

export const projectCareerCardsQuery = (slug: string) =>
  queryOptions({
    queryKey: ['project-detail', slug, 'career-cards'],
    queryFn: ({ signal }) => get<ProjectCareerCard[]>(`${projectPath(slug)}/career-cards`, signal),
  });

export const projectResourceTopicsQuery = (slug: string, jobRole: MaterialJobRole) =>
  queryOptions({
    queryKey: ['project-detail', slug, 'resource-topics', jobRole],
    queryFn: ({ signal }) =>
      get<ProjectResourceTopics>(`${projectPath(slug)}/resource-topics`, signal, { jobRole }),
  });

export const projectTopicResourcesQuery = (
  slug: string,
  topicId: string,
  jobRole: MaterialJobRole,
) =>
  queryOptions({
    queryKey: ['project-detail', slug, 'resources', topicId, jobRole],
    queryFn: ({ signal }) =>
      get<ProjectTopicResources>(`${projectPath(slug)}/resources`, signal, { topicId, jobRole }),
  });
