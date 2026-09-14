// TODO 대체 GET /api/v1/news의 type 쿼리 파라미터 enum
export const NEWS_FILTERS = ['ALL', 'NOTICE', 'EVENT'] as const;

export type NewsFilter = (typeof NEWS_FILTERS)[number];

export const DEFAULT_NEWS_FILTER: NewsFilter = 'ALL';

// TODO 서버에서 정의한 타입이 LATEST 하나라 업데이트되는 대로 추가 -> 현재 string이라 enum으로 확정 되면 대체
export const NEWS_SORTS = ['LATEST'] as const;

export type NewsSort = (typeof NEWS_SORTS)[number];

export const DEFAULT_NEWS_SORT: NewsSort = 'LATEST';

// 사전에 정의한 값이 아닐 경우 undefined로 덮기 위한 용도의 타입 가드
export const isNewsFilter = (value: unknown): value is NewsFilter =>
  NEWS_FILTERS.some((filter) => filter === value);

export const isNewsSort = (value: unknown): value is NewsSort =>
  NEWS_SORTS.some((sort) => sort === value);

// TODO 대체 NewsListItem.type (명세는 null도 포함)
export type NewsType = 'NOTICE' | 'EVENT';

// TODO 대체 NewsListItem
export interface NewsSummary {
  id: number;
  type: NewsType;
  title: string;
  summary: string;
  publishedAt: string;
}

// TODO 대체 NewsAuthor
export interface NewsAuthor {
  userId: number;
  name: string;
}

// TODO 대체 NewsCta
export interface NewsCta {
  label: string;
  url: string;
}

// TODO 대체 NewsNavItem
export interface NewsNavItem {
  id: number;
  title: string;
  publishedAt: string;
}

// TODO 대체 NewsDetail
export interface NewsDetail extends Omit<NewsSummary, 'summary'> {
  body: string;
  author: NewsAuthor;
  cta: NewsCta | null;
  previous: NewsNavItem | null;
  next: NewsNavItem | null;
}
