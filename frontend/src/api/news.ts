import { infiniteQueryOptions, queryOptions } from '@tanstack/react-query';
import {
  type NewsDetail,
  type NewsFilter,
  type NewsListOptions,
  type NewsSort,
  type NewsSummary,
} from '@/types/news';
import { httpClient } from '@/utils/client';
import type {
  NewsFindAllSuccessResponse,
  NewsFindDetailSuccessResponse,
} from '@/api/generated/schema';

const NEWS_PATH = '/api/v1/news';

type NewsListResponse = NewsFindAllSuccessResponse;

export async function fetchNewsList(
  type: NewsFilter,
  sort: NewsSort,
  options?: NewsListOptions,
): Promise<NewsSummary[]> {
  const body = await httpClient<NewsListResponse>(NEWS_PATH, {
    method: 'get',
    // ky가 값이 undefined인 옵션은 쿼리에서 뺀다.
    searchParams: { type, sort, ...options },
  });
  if (!body) throw new Error(`소식 목록 응답이 비어 있습니다: ${NEWS_PATH}`);

  return body.data;
}

export async function fetchNewsDetail(newsId: number): Promise<NewsDetail> {
  const path = `${NEWS_PATH}/${newsId}`;

  const body = await httpClient<NewsFindDetailSuccessResponse>(path, { method: 'get' });
  if (!body) throw new Error(`소식 상세 응답이 비어 있습니다: ${path}`);

  return body.data;
}

export const newsListQueryOptions = (type: NewsFilter, sort: NewsSort, options?: NewsListOptions) =>
  queryOptions({
    queryKey: ['news', { type, sort, ...options }],
    queryFn: () => fetchNewsList(type, sort, options),
  });

export const newsInfiniteQueryOptions = (type: NewsFilter, sort: NewsSort) =>
  infiniteQueryOptions({
    queryKey: ['news', { type, sort }],
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam }) => fetchNewsPage(type, sort, pageParam),
    getNextPageParam: (last) =>
      last.meta?.hasNext && last.meta.nextCursor ? last.meta.nextCursor : undefined,
  });

async function fetchNewsPage(type: NewsFilter, sort: NewsSort, cursor?: string) {
  const body = await httpClient<NewsListResponse>(NEWS_PATH, {
    method: 'get',
    searchParams: { type, sort, ...(cursor ? { cursor } : {}) },
  });
  if (!body) throw new Error(`소식 목록 응답이 비어 있습니다: ${NEWS_PATH}`);
  return {
    ...body,
    meta: body.meta ?? { nextCursor: null, hasNext: false },
  };
}

export const newsDetailQueryOptions = (newsId: number) =>
  queryOptions({
    queryKey: ['news', newsId],
    queryFn: () => fetchNewsDetail(newsId),
  });
