import { NEWS_FILTERS, NEWS_SORTS } from '@/constants/news';
import type { NewsDetailData, NewsListItemData } from '@/types/api';

export type NewsFilter = (typeof NEWS_FILTERS)[number];

export type NewsSort = (typeof NEWS_SORTS)[number];

// 사전에 정의한 값이 아닐 경우 undefined로 덮기 위한 용도의 타입 가드
export const isNewsFilter = (value: unknown): value is NewsFilter =>
  NEWS_FILTERS.some((filter) => filter === value);

export const isNewsSort = (value: unknown): value is NewsSort =>
  NEWS_SORTS.some((sort) => sort === value);

/** 소식 유형. 스키마의 `type` 값을 그대로 쓴다. */
export type NewsType = NewsListItemData['type'];

/** 이벤트 상태. 목록 조회의 eventStatus 쿼리 파라미터로도 쓴다. */
export type NewsEventStatus = NonNullable<NewsListItemData['eventStatus']>;

export interface NewsListOptions {
  eventStatus?: NewsEventStatus;
  size?: number;
  cursor?: string;
}

/** 목록 한 건. `GET /api/v1/news` */
export type NewsSummary = NewsListItemData;

/** 상세. `GET /api/v1/news/{newsId}` */
export type NewsDetail = NewsDetailData;

export type NewsAuthor = NewsDetail['author'];
export type NewsCta = NonNullable<NewsDetail['cta']>;
export type NewsNavItem = NonNullable<NewsDetail['previous']>;
