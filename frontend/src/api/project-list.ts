import { queryOptions } from '@tanstack/react-query';
import ky from 'ky';
import { getApiUrl } from '@/api/url';

// 목록 PDF에서 확인된 표시 필드만 사용한다. 상세 페이지의 SSG mock 타입과는 별개다.
export interface ProjectListItem {
  id: number;
  title: string;
  tagline: string;
  thumbnailUrl: string | null;
  cohort: number;
}

export async function fetchProjectList(signal?: AbortSignal): Promise<ProjectListItem[]> {
  const response = await ky
    .get(getApiUrl('/api/v1/projects'), { signal, retry: 0, credentials: 'omit' })
    .json<{
      status: string;
      data: ProjectListItem[];
    }>();

  return response.data;
}

export const projectListQueryOptions = () =>
  queryOptions({
    queryKey: ['project-list'],
    queryFn: ({ signal }) => fetchProjectList(signal),
  });
