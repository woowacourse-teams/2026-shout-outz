import { queryOptions } from '@tanstack/react-query';
import type { ProjectFindDetailSuccessResponse } from '@/api/generated/schema';
import type { ProjectDetail } from '@/types/project';
import { httpClient } from '@/utils/client';

export type { ProjectDetail };

const PROJECTS_PATH = '/api/v1/projects';

export async function fetchProjectDetail(id: string, signal?: AbortSignal): Promise<ProjectDetail> {
  const path = `${PROJECTS_PATH}/${id}`;

  const body = await httpClient<ProjectFindDetailSuccessResponse>(path, {
    method: 'get',
    signal,
    retry: 0,
  });
  if (!body) throw new Error(`프로젝트 상세 응답이 비어 있습니다: ${path}`);

  return body.data;
}

export const projectDetailQueryOptions = (id: string) =>
  queryOptions({
    queryKey: ['project-detail', id],
    queryFn: ({ signal }) => fetchProjectDetail(id, signal),
    staleTime: 60_000,
  });
