import type { NewsFilter, NewsSort } from '@/types/news';

// TODO 대체 GET /api/v1/news의 type 쿼리 파라미터 enum
export const NEWS_FILTERS = ['ALL', 'NOTICE', 'EVENT'] as const;

export const DEFAULT_NEWS_FILTER: NewsFilter = 'ALL';

// TODO 서버에서 정의한 타입이 LATEST 하나라 업데이트되는 대로 추가 -> 현재 string이라 enum으로 확정 되면 대체
export const NEWS_SORTS = ['LATEST'] as const;

export const DEFAULT_NEWS_SORT: NewsSort = 'LATEST';
