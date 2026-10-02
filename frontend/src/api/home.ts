import { queryOptions } from '@tanstack/react-query';
import type {
  HomeBannerFindAllSuccessResponse,
  HomeStatisticsSuccessResponse,
} from '@/api/generated/schema';
import { type HomeBanner, type HomeStatistics } from '@/types/home';
import { httpClient } from '@/utils/client';

const HOME_STATISTICS_PATH = '/api/v1/home/statistics';
const HOME_BANNERS_PATH = '/api/v1/home/banners';

export async function fetchHomeStatistics(signal?: AbortSignal): Promise<HomeStatistics> {
  const body = await httpClient<HomeStatisticsSuccessResponse>(HOME_STATISTICS_PATH, {
    method: 'get',
    signal,
  });
  if (!body) throw new Error(`홈 통계 응답이 비어 있습니다: ${HOME_STATISTICS_PATH}`);

  return body.data;
}

export const homeStatisticsQueryOptions = () =>
  queryOptions({
    queryKey: ['home', 'statistics'],
    queryFn: ({ signal }) => fetchHomeStatistics(signal),
  });

/** 활성 배너를 표시 순서대로 전체 조회한다. */
export async function fetchHomeBanners(signal?: AbortSignal): Promise<HomeBanner[]> {
  const body = await httpClient<HomeBannerFindAllSuccessResponse>(HOME_BANNERS_PATH, {
    method: 'get',
    signal,
  });
  if (!body) throw new Error(`홈 배너 응답이 비어 있습니다: ${HOME_BANNERS_PATH}`);

  return body.data;
}

export const homeBannersQueryOptions = () =>
  queryOptions({
    queryKey: ['home', 'banners'],
    queryFn: ({ signal }) => fetchHomeBanners(signal),
  });
