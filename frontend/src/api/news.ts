import { queryOptions } from '@tanstack/react-query';

import { getApiUrl } from '@/api/url';
import type { NewsDetail, NewsFilter, NewsSummary } from '@/types/news';
import { httpClient } from '@/utils/client';

interface ApiBody<T> {
  data: T;
}

export async function fetchNewsList(type: NewsFilter): Promise<NewsSummary[]> {
  const url = getApiUrl('/api/v1/news');
  url.searchParams.set('type', type);

  const body = await httpClient<ApiBody<NewsSummary[]>>(url.toString(), { method: 'get' });
  if (!body) throw new Error(`소식 목록 응답이 비어 있습니다: ${url.pathname}`);

  return body.data;
}

export async function fetchNewsDetail(newsId: number): Promise<NewsDetail> {
  const url = getApiUrl(`/api/v1/news/${newsId}`);

  const body = await httpClient<ApiBody<NewsDetail>>(url.toString(), { method: 'get' });
  if (!body) throw new Error(`소식 상세 응답이 비어 있습니다: ${url.pathname}`);

  return body.data;
}

export const newsListQueryOptions = (type: NewsFilter) =>
  queryOptions({
    queryKey: ['news', { type }],
    queryFn: () => fetchNewsList(type),
  });

export const newsDetailQueryOptions = (newsId: number) =>
  queryOptions({
    queryKey: ['news', newsId],
    queryFn: () => fetchNewsDetail(newsId),
  });
