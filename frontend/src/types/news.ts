export const NEWS_FILTERS = ['ALL', 'NOTICE', 'EVENT'] as const;

export type NewsFilter = (typeof NEWS_FILTERS)[number];

export const DEFAULT_NEWS_FILTER: NewsFilter = 'ALL';

// TODO 서버에서 정의한 타입이 LATEST 하나라 업데이트되는 대로 추가
export const NEWS_SORTS = ['LATEST'] as const;

export type NewsSort = (typeof NEWS_SORTS)[number];

export const DEFAULT_NEWS_SORT: NewsSort = 'LATEST';

// 사전에 정의한 값이 아닐 경우 undefined로 덮기 위한 용도의 타입 가드
export const isNewsFilter = (value: unknown): value is NewsFilter =>
  NEWS_FILTERS.some((filter) => filter === value);

export const isNewsSort = (value: unknown): value is NewsSort =>
  NEWS_SORTS.some((sort) => sort === value);
