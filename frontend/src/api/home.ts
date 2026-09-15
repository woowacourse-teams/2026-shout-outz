import { queryOptions } from '@tanstack/react-query';
import { type HomeStatistics } from '@/types/home';
import { type ApiSuccessBody, httpClient } from '@/utils/client';

const HOME_STATISTICS_PATH = '/api/v1/home/statistics';

export async function fetchHomeStatistics(): Promise<HomeStatistics> {
  const body = await httpClient<ApiSuccessBody<HomeStatistics>>(HOME_STATISTICS_PATH, {
    method: 'get',
  });
  if (!body) throw new Error(`홈 통계 응답이 비어 있습니다: ${HOME_STATISTICS_PATH}`);

  return body.data;
}

export const homeStatisticsQueryOptions = () =>
  queryOptions({
    queryKey: ['home', 'statistics'],
    queryFn: fetchHomeStatistics,
  });
