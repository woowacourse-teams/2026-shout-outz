import { infiniteQueryOptions } from '@tanstack/react-query';
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
export function nextCursor(page: { meta: CursorMeta }, previous: (string | undefined)[]) {
  const cursor = page.meta.nextCursor;
  return page.meta.hasNext && cursor && !previous.includes(cursor) ? cursor : undefined;
}
export function feedsQuery(sort: FeedSort, categoryId?: number, size = 20) {
  return infiniteQueryOptions({
    queryKey: ['feeds', { sort, categoryId, size }],
    initialPageParam: undefined as string | undefined,
    queryFn: async ({ pageParam, signal }) => {
      const result = await httpClient<Envelope<Feed[]>>('/api/v1/posts', {
        method: 'get',
        signal,
        searchParams: {
          sort,
          size,
          ...(categoryId === undefined ? {} : { categoryId }),
          ...(pageParam ? { cursor: pageParam } : {}),
        },
      });
      if (!result) throw new Error('피드 응답이 비어 있습니다.');
      return result;
    },
    getNextPageParam: (last, _pages, _param, params) => nextCursor(last, params),
  });
}
