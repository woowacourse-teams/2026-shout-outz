import { infiniteQueryOptions, keepPreviousData, queryOptions } from '@tanstack/react-query';
import type { ProjectFilterOptionsSuccessResponse } from '@/api/generated/schema';
import { DEFAULT_PROJECT_FILTER, PROJECT_PAGE_SIZE } from '@/constants/project';
import {
  type ProjectFilter,
  type ProjectFilterOptions,
  type ProjectListMeta,
  type ProjectSummary,
} from '@/types/project';
import { httpClient, type ApiSuccessBody } from '@/utils/client';

const PROJECTS_PATH = '/api/v1/projects';
const PROJECT_FILTERS_PATH = '/api/v1/projects/filters';

const EMPTY_META: ProjectListMeta = { nextCursor: null, hasNext: false, totalCount: 0 };

function toSearchParams(filter: ProjectFilter) {
  const keyword = filter.keyword.trim();

  return {
    ...(keyword ? { keyword } : {}),
    ...(filter.cohorts.length ? { cohorts: filter.cohorts.join(',') } : {}),
    ...(filter.techTagIds.length ? { techTagIds: filter.techTagIds.join(',') } : {}),
    sort: filter.sort,
  };
}

export async function fetchProjectListPage(
  filter: ProjectFilter,
  cursor?: string,
  signal?: AbortSignal,
) {
  const response = await httpClient<ApiSuccessBody<ProjectSummary[], ProjectListMeta>>(
    PROJECTS_PATH,
    {
      method: 'get',
      signal,
      retry: 0,
      searchParams: {
        ...toSearchParams(filter),
        size: PROJECT_PAGE_SIZE,
        ...(cursor ? { cursor } : {}),
      },
    },
  );

  if (!response || !Array.isArray(response.data)) {
    throw new Error(`프로젝트 목록 응답이 비어 있습니다: ${PROJECTS_PATH}`);
  }

  return {
    projects: response.data,
    meta: response.meta ?? { ...EMPTY_META, totalCount: response.data.length },
  };
}

/** 전체 프로젝트를 받는다. 상세 페이지 SSG 경로를 모으는 용도라 `nextCursor`를 따라 끝까지 요청한다. */
export async function fetchProjectList(signal?: AbortSignal): Promise<ProjectSummary[]> {
  const projects: ProjectSummary[] = [];
  let cursor: string | undefined;

  do {
    const page = await fetchProjectListPage(DEFAULT_PROJECT_FILTER, cursor, signal);
    projects.push(...page.projects);
    cursor = page.meta.hasNext ? (page.meta.nextCursor ?? undefined) : undefined;
  } while (cursor);

  return projects;
}

export const projectListQueryOptions = () =>
  queryOptions({
    queryKey: ['project-list'],
    queryFn: ({ signal }) => fetchProjectList(signal),
  });

export const projectListInfiniteQueryOptions = (filter: ProjectFilter) =>
  infiniteQueryOptions({
    queryKey: ['project-list', filter],
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam, signal }) => fetchProjectListPage(filter, pageParam, signal),
    getNextPageParam: (last) =>
      last.meta?.hasNext && last.meta.nextCursor ? last.meta.nextCursor : undefined,
  });

export async function fetchProjectFilterOptions(
  filter: ProjectFilter,
  signal?: AbortSignal,
): Promise<ProjectFilterOptions> {
  const { sort, ...conditions } = toSearchParams(filter);
  void sort;

  const response = await httpClient<ProjectFilterOptionsSuccessResponse>(PROJECT_FILTERS_PATH, {
    method: 'get',
    signal,
    searchParams: conditions,
  });
  if (!response) throw new Error(`프로젝트 필터 응답이 비어 있습니다: ${PROJECT_FILTERS_PATH}`);

  return response.data;
}

export const projectFilterOptionsQueryOptions = (filter: ProjectFilter) =>
  queryOptions({
    queryKey: ['project-filters', filter.keyword.trim(), filter.cohorts, filter.techTagIds],
    queryFn: ({ signal }) => fetchProjectFilterOptions(filter, signal),
    placeholderData: keepPreviousData,
  });
