// TODO 대체 GET /api/v1/feeds의 sort 쿼리 파라미터 enum
export type FeedSort = 'LATEST' | 'POPULAR';

export interface FeedListParams {
  sort: FeedSort;
  size: number;
}

// TODO 대체 AuthorSummary
export interface FeedAuthor {
  handle: string;
  displayName: string;
  userType: 'GENERAL' | 'WOOWACOURSE_CREW' | 'WOOWACOURSE_COACH';
  track: 'BACKEND' | 'ANDROID' | 'FRONTEND' | null;
  cohort: number | null;
  avatarUrl: string | null;
}

// TODO 대체 FeedCategory
export interface FeedCategory {
  categoryId: number;
  slug: string;
  displayName: string;
  type: 'GENERAL' | 'EVENT';
}

// TODO 대체 FeedMedia
export interface FeedMedia {
  /** 작성 API의 미디어 ID. 조회 응답에서는 공개 URL만 내려올 수 있다. */
  mediaId?: number;
  displayOrder: number;
  /** 조회 응답의 공개 미디어 URL */
  url?: string;
}

// TODO 대체 Feed
export interface Feed {
  feedId: number;
  content: string;
  author: FeedAuthor;
  categories: FeedCategory[];
  media: FeedMedia[];
  createdAt: string;
  updatedAt: string;
}
