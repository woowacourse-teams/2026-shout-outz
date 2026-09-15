import { infiniteQueryOptions, queryOptions } from '@tanstack/react-query';
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
  postId: number;
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

export async function fetchFeed(postId: number, signal?: AbortSignal) {
  const response = await httpClient<{ status: 'success'; data: Feed }>(`/api/v1/posts/${postId}`, {
    method: 'get',
    signal,
  });

  if (!response) throw new Error('피드 응답이 비어 있습니다.');
  return response.data;
}

export function feedQuery(postId: number) {
  return queryOptions({
    queryKey: ['feed', postId],
    queryFn: ({ signal }) => fetchFeed(postId, signal),
  });
}
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
  const response = await httpClient<Envelope<Feed[]>>('/api/v1/posts', {
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
