import { queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import type { CategoryFindAllSuccessResponse } from '@/api/generated/schema';
import type { CategoryItem } from '@/types/api';

export type Category = CategoryItem;

export async function fetchCategories(signal?: AbortSignal) {
  const response = await httpClient<CategoryFindAllSuccessResponse>('/api/v1/categories', {
    method: 'get',
    signal,
  });
  if (!response) throw new Error('카테고리 응답이 비어 있습니다.');
  return response.data;
}

export const categoriesQuery = queryOptions({
  queryKey: ['categories'],
  queryFn: ({ signal }) => fetchCategories(signal),
});
