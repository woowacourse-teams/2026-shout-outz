import { infiniteQueryOptions, mutationOptions, queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';

export interface CursorMeta {
  nextCursor: string | null;
  hasNext: boolean;
}
export interface Envelope<T> {
  status: 'success';
  data: T;
  meta: CursorMeta;
}
export type FeedSort = 'LATEST' | 'POPULAR';
export interface Feed {
  feedId: number;
  content: string;
  author: {
    handle: string;
    displayName: string;
    userType: string;
    track: string | null;
    cohort: number | null;
    avatarImageId: number | null;
  };
  categories: { categoryId: number; slug: string; displayName: string; type: string }[];
  media: { mediaId: number; displayOrder: number }[];
  createdAt: string;
  updatedAt: string;
}

export async function fetchFeed(feedId: number, signal?: AbortSignal) {
  const response = await httpClient<{ status: 'success'; data: Feed }>(`/api/v1/feeds/${feedId}`, {
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

export interface SaveFeedInput {
  content: string;
  categoryIds: number[];
  mediaIds: number[];
}

export async function createFeed(input: SaveFeedInput) {
  const response = await httpClient<{ status: 'success'; data: Feed }>('/api/v1/feeds', {
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
  const response = await httpClient<{ status: 'success'; data: Feed }>(`/api/v1/feeds/${feedId}`, {
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
  const response = await httpClient<Envelope<Feed[]>>('/api/v1/feeds', {
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
