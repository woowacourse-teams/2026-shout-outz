import type { HomeBannerItem, HomeStatisticsData } from '@/types/api';

/** 홈 통계. `GET /api/v1/home/statistics` */
export type HomeStatistics = HomeStatisticsData;

/** 홈 배너 한 건. `GET /api/v1/home/banners` */
export type HomeBanner = HomeBannerItem;
