import { queryOptions } from '@tanstack/react-query';
import { type ProjectSummary } from '@/types/project';
import { type ApiSuccessBody, httpClient } from '@/utils/client';

const PROJECTS_PATH = '/api/v1/projects';

export async function fetchProjectList(signal?: AbortSignal): Promise<ProjectSummary[]> {
  const response = await httpClient<ApiSuccessBody<ProjectSummary[]>>(PROJECTS_PATH, {
    method: 'get',
    signal,
    retry: 0,
  });

  if (!response || !Array.isArray(response.data)) {
    throw new Error(`프로젝트 목록 응답이 비어 있습니다: ${PROJECTS_PATH}`);
  }

  return response.data;
}

export const projectListQueryOptions = () =>
  queryOptions({
    queryKey: ['project-list'],
    queryFn: ({ signal }) => fetchProjectList(signal),
  });
