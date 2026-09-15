import { queryOptions } from '@tanstack/react-query';
import { type HomeStatistics } from '@/types/home';

export async function fetchHomeStatistics(): Promise<HomeStatistics> {
  // TODO 테스트 검토 후 구현
}

export const homeStatisticsQueryOptions = () =>
  queryOptions({
    queryKey: ['home', 'statistics'],
    queryFn: fetchHomeStatistics,
  });
