import { queryOptions } from '@tanstack/react-query';
import { type Feed, type FeedListParams } from '@/types/feed';
import { type ApiSuccessBody, httpClient } from '@/utils/client';

const FEEDS_PATH = '/api/v1/feeds';

export async function fetchFeedList({ sort, size }: FeedListParams): Promise<Feed[]> {
  const body = await httpClient<
    ApiSuccessBody<Feed[], { nextCursor: string | null; hasNext: boolean }>
  >(FEEDS_PATH, {
    method: 'get',
    searchParams: { sort, size },
  });
  if (!body) throw new Error(`피드 목록 응답이 비어 있습니다: ${FEEDS_PATH}`);

  return body.data;
}

export const feedListQueryOptions = (params: FeedListParams) =>
  queryOptions({
    queryKey: ['feeds', params],
    queryFn: () => fetchFeedList(params),
  });
