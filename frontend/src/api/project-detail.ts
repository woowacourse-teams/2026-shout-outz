import { queryOptions } from '@tanstack/react-query';
import type { ProjectFindDetailSuccessResponse } from '@/api/generated/schema';
import type { ProjectDetail } from '@/types/project';
import { httpClient } from '@/utils/client';

export type { ProjectDetail };

const PROJECTS_PATH = '/api/v1/projects';

/**
 * slug로 상세를 조회한다.
 *
 * 숫자 id로는 받지 않는다(405). 수정·리액션·댓글처럼 이후 요청은 이 응답의 `id`를 쓴다.
 */
export async function fetchProjectDetail(
  slug: string,
  signal?: AbortSignal,
): Promise<ProjectDetail> {
  const path = `${PROJECTS_PATH}/@${slug}`;

  const body = await httpClient<ProjectFindDetailSuccessResponse>(path, {
    method: 'get',
    signal,
    retry: 0,
  });
  if (!body) throw new Error(`프로젝트 상세 응답이 비어 있습니다: ${path}`);

  return body.data;
}

export const projectDetailQueryOptions = (slug: string) =>
  queryOptions({
    queryKey: ['project-detail', slug],
    queryFn: ({ signal }) => fetchProjectDetail(slug, signal),
    staleTime: 60_000,
  });
