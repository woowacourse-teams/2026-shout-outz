import { queryOptions } from '@tanstack/react-query';
import ky from 'ky';

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
    .get(new URL('/api/v1/projects', window.location.origin), { signal, retry: 0 })
    .json<{
      status: string;
      data: ProjectListItem[];
    }>();

  if (response.status !== 'success' || !Array.isArray(response.data)) {
    throw new Error('프로젝트 목록 응답을 확인할 수 없습니다.');
  }

  return response.data;
}

export const projectListQueryOptions = () =>
  queryOptions({
    queryKey: ['project-list'],
    queryFn: ({ signal }) => fetchProjectList(signal),
  });
