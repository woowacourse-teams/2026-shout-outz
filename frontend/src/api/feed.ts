import { queryOptions } from '@tanstack/react-query';
import { type Feed, type FeedListParams } from '@/types/feed';

export async function fetchFeedList(params: FeedListParams): Promise<Feed[]> {}

export const feedListQueryOptions = (params: FeedListParams) =>
  queryOptions({
    queryKey: ['feeds', params],
    queryFn: () => fetchFeedList(params),
  });
