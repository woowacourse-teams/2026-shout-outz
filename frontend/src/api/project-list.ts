import { infiniteQueryOptions, queryOptions } from '@tanstack/react-query';
import type { ProjectFindAllSuccessResponse } from '@/api/generated/schema';
import { type ProjectListMeta, type ProjectSummary } from '@/types/project';
import { httpClient } from '@/utils/client';

const PROJECTS_PATH = '/api/v1/projects';

const EMPTY_META: ProjectListMeta = { nextCursor: null, hasNext: false, totalCount: 0 };

export async function fetchProjectListPage(cursor?: string, signal?: AbortSignal) {
  const response = await httpClient<ProjectFindAllSuccessResponse>(PROJECTS_PATH, {
    method: 'get',
    signal,
    retry: 0,
    searchParams: cursor ? { cursor } : undefined,
  });

  if (!response || !Array.isArray(response.data)) {
    throw new Error(`프로젝트 목록 응답이 비어 있습니다: ${PROJECTS_PATH}`);
  }

  return {
    projects: response.data,
    meta: response.meta ?? { ...EMPTY_META, totalCount: response.data.length },
  };
}

export async function fetchProjectList(signal?: AbortSignal): Promise<ProjectSummary[]> {
  const page = await fetchProjectListPage(undefined, signal);
  return page.projects;
}

export const projectListQueryOptions = () =>
  queryOptions({
    queryKey: ['project-list'],
    queryFn: ({ signal }) => fetchProjectList(signal),
  });

export const projectListInfiniteQueryOptions = () =>
  infiniteQueryOptions({
    queryKey: ['project-list'],
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam, signal }) => fetchProjectListPage(pageParam, signal),
    getNextPageParam: (last) =>
      last.meta?.hasNext && last.meta.nextCursor ? last.meta.nextCursor : undefined,
  });
