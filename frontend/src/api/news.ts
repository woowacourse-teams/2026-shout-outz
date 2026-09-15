import { queryOptions } from '@tanstack/react-query';
import {
  type NewsDetail,
  type NewsFilter,
  type NewsListOptions,
  type NewsSort,
  type NewsSummary,
} from '@/types/news';
import { httpClient } from '@/utils/client';

const NEWS_PATH = '/api/v1/news';

interface ApiBody<T> {
  data: T;
}

// TODO 테스트 검토 후 options(eventStatus, size)를 searchParams에 반영
export async function fetchNewsList(
  type: NewsFilter,
  sort: NewsSort,
  options?: NewsListOptions,
): Promise<NewsSummary[]> {
  const body = await httpClient<ApiBody<NewsSummary[]>>(NEWS_PATH, {
    method: 'get',
    searchParams: { type, sort },
  });
  if (!body) throw new Error(`소식 목록 응답이 비어 있습니다: ${NEWS_PATH}`);

  return body.data;
}

export async function fetchNewsDetail(newsId: number): Promise<NewsDetail> {
  const path = `${NEWS_PATH}/${newsId}`;

  const body = await httpClient<ApiBody<NewsDetail>>(path, { method: 'get' });
  if (!body) throw new Error(`소식 상세 응답이 비어 있습니다: ${path}`);

  return body.data;
}

export const newsListQueryOptions = (type: NewsFilter, sort: NewsSort, options?: NewsListOptions) =>
  queryOptions({
    queryKey: ['news', { type, sort, ...options }],
    queryFn: () => fetchNewsList(type, sort, options),
  });

export const newsDetailQueryOptions = (newsId: number) =>
  queryOptions({
    queryKey: ['news', newsId],
    queryFn: () => fetchNewsDetail(newsId),
  });
