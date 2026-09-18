import { queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';

export interface Category {
  categoryId: number;
  slug: string;
  displayName: string;
  type: 'GENERAL' | 'EVENT';
  displayOrder: number;
}

export async function fetchCategories(signal?: AbortSignal) {
  const response = await httpClient<{ status: 'success'; data: Category[] }>('/api/v1/categories', {
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
