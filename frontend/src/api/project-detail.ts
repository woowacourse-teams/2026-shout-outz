import { queryOptions } from '@tanstack/react-query';
import ky from 'ky';
import { getApiUrl } from '@/api/url';
import type { ProjectFindDetailSuccessResponse } from '@/api/generated/schema';
import type { ProjectDetail } from '@/types/project';

export type { ProjectDetail };

export async function fetchProjectDetail(id: string, signal?: AbortSignal): Promise<ProjectDetail> {
  const response = await ky
    .get(getApiUrl(`/api/v1/projects/${id}`), { signal, retry: 0, credentials: 'omit' })
    .json<ProjectFindDetailSuccessResponse>();

  return response.data;
}

export const projectDetailQueryOptions = (id: string) =>
  queryOptions({
    queryKey: ['project-detail', id],
    queryFn: ({ signal }) => fetchProjectDetail(id, signal),
    staleTime: 60_000,
  });
