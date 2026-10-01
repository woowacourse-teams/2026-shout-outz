import { queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import type { CategoryFindAllSuccessResponse } from '@/api/generated/schema';
import type { FeedType } from '@/types/feed';

export async function fetchCategories(feedType: FeedType, signal?: AbortSignal) {
  const response = await httpClient<CategoryFindAllSuccessResponse>('/api/v1/categories', {
    method: 'get',
    signal,
    searchParams: { feedType },
  });
  if (!response) throw new Error('카테고리 응답이 비어 있습니다.');
  return response.data;
}

export const categoriesQuery = (feedType: FeedType) =>
  queryOptions({
    queryKey: ['categories', feedType],
    queryFn: ({ signal }) => fetchCategories(feedType, signal),
  });
