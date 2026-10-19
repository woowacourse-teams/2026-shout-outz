import { infiniteQueryOptions, queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import { nextCursor } from '@/apis/feed';
import type {
  MagazineFindAllSuccessResponse,
  MagazineFindSuccessResponse,
} from '@/api/generated/schema';
import type { MagazineDetailData, MagazineListItemData } from '@/types/api';

export type Magazine = MagazineDetailData;
export type MagazineListItem = MagazineListItemData;

export async function fetchMagazine(magazineId: number, signal?: AbortSignal) {
  const response = await httpClient<MagazineFindSuccessResponse>(
    `/api/v1/magazines/${magazineId}`,
    { method: 'get', signal },
  );

  if (!response?.data) throw new Error('매거진 응답이 비어 있습니다.');
  return response.data;
}

export function magazineQuery(magazineId: number) {
  return queryOptions({
    queryKey: ['magazine', magazineId],
    queryFn: ({ signal }) => fetchMagazine(magazineId, signal),
  });
}

interface FetchMagazinesParams {
  cursor?: string;
  size: number;
  signal?: AbortSignal;
}

export async function fetchMagazines({ cursor, size, signal }: FetchMagazinesParams) {
  const response = await httpClient<MagazineFindAllSuccessResponse>('/api/v1/magazines', {
    method: 'get',
    signal,
    searchParams: { size, ...(cursor ? { cursor } : {}) },
  });

  if (!response) throw new Error('매거진 응답이 비어 있습니다.');
  return {
    ...response,
    meta: response.meta ?? { hasNext: false, nextCursor: null, totalCount: 0 },
  };
}

export function magazinesQuery(size = 20) {
  return infiniteQueryOptions({
    queryKey: ['magazines', { size }],
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam, signal }) => fetchMagazines({ cursor: pageParam, size, signal }),
    getNextPageParam: (last, _pages, _param, params) => nextCursor(last, params),
  });
}
