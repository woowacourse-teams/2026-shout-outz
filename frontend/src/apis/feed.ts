import { infiniteQueryOptions, mutationOptions, queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import type {
  FeedFindAllSuccessResponse,
  FeedFindSuccessResponse,
  FeedSaveRequest,
  FeedSaveSuccessResponse,
  FeedUpdateSuccessResponse,
} from '@/api/generated/schema';
import type { CursorMeta } from '@/types/api';
import type { FeedSort } from '@/types/feed';

export type { Feed, FeedSort } from '@/types/feed';
export type { CursorMeta };

export async function fetchFeed(feedId: number, signal?: AbortSignal) {
  const response = await httpClient<FeedFindSuccessResponse>(`/api/v1/feeds/${feedId}`, {
    method: 'get',
    signal,
  });

  if (!response) throw new Error('피드 응답이 비어 있습니다.');
  return response.data;
}

export function feedQuery(feedId: number) {
  return queryOptions({
    queryKey: ['feed', feedId],
    queryFn: ({ signal }) => fetchFeed(feedId, signal),
  });
}

/**
 * 피드 작성·수정 요청 본문.
 *
 * 스키마는 categoryIds·mediaIds를 `(object | boolean | string | number)[]`로 뽑는다(생성기가
 * 배열 원소 타입을 못 읽은 결과다). 서버가 받는 값은 ID 숫자라 number[]로 좁혀 쓴다.
 */
export interface SaveFeedInput extends Omit<FeedSaveRequest, 'categoryIds' | 'mediaIds'> {
  categoryIds: number[];
  mediaIds: number[];
}

export async function createFeed(input: SaveFeedInput) {
  const response = await httpClient<FeedSaveSuccessResponse>('/api/v1/feeds', {
    method: 'post',
    json: input,
  });
  if (!response) throw new Error('피드 응답이 비어 있습니다.');
  return response.data;
}

export const createFeedMutation = mutationOptions({
  mutationFn: createFeed,
  retry: false,
});
export async function updateFeed(feedId: number, input: SaveFeedInput) {
  const response = await httpClient<FeedUpdateSuccessResponse>(`/api/v1/feeds/${feedId}`, {
    method: 'put',
    json: input,
  });
  if (!response) throw new Error('피드 응답이 비어 있습니다.');
  return response.data;
}

export const updateFeedMutation = (feedId: number) =>
  mutationOptions({
    mutationFn: (input: SaveFeedInput) => updateFeed(feedId, input),
    retry: false,
  });

export async function deleteFeed(feedId: number) {
  await httpClient(`/api/v1/feeds/${feedId}`, { method: 'delete' });
}

export const deleteFeedMutation = (feedId: number) =>
  mutationOptions({
    mutationFn: () => deleteFeed(feedId),
    retry: false,
  });

export function nextCursor(page: { meta: CursorMeta }, previous: (string | undefined)[]) {
  const cursor = page.meta.nextCursor;
  return page.meta.hasNext && cursor && !previous.includes(cursor) ? cursor : undefined;
}

interface FetchFeedsParams {
  sort: FeedSort;
  categoryId?: number;
  cursor?: string;
  size: number;
  signal?: AbortSignal;
}

export async function fetchFeeds({ sort, categoryId, cursor, size, signal }: FetchFeedsParams) {
  const response = await httpClient<FeedFindAllSuccessResponse>('/api/v1/feeds', {
    method: 'get',
    signal,
    searchParams: {
      sort,
      size,
      ...(categoryId === undefined ? {} : { categoryId }),
      ...(cursor ? { cursor } : {}),
    },
  });

  if (!response) throw new Error('피드 응답이 비어 있습니다.');
  return response;
}

export function feedsQuery(sort: FeedSort, categoryId?: number, size = 20) {
  return infiniteQueryOptions({
    queryKey: ['feeds', { sort, categoryId, size }],
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam, signal }) =>
      fetchFeeds({ sort, categoryId, cursor: pageParam, size, signal }),
    getNextPageParam: (last, _pages, _param, params) => nextCursor(last, params),
  });
}
