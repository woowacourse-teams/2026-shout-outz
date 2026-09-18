import { queryOptions } from '@tanstack/react-query';
import { type ProjectSummary } from '@/types/project';
import { type ApiSuccessBody, httpClient } from '@/utils/client';

const PROJECTS_PATH = '/api/v1/projects';

export async function fetchProjectList(): Promise<ProjectSummary[]> {
  const body = await httpClient<ApiSuccessBody<ProjectSummary[]>>(PROJECTS_PATH, { method: 'get' });
  if (!body) throw new Error(`프로젝트 목록 응답이 비어 있습니다: ${PROJECTS_PATH}`);

  return body.data;
}

export const projectListQueryOptions = () =>
  queryOptions({
    queryKey: ['project-list'],
    queryFn: fetchProjectList,
  });
