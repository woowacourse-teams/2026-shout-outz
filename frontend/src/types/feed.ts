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
  userType: string;
  track: string | null;
  cohort: number | null;
  avatarImageId: number | null;
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
  mediaId: number;
  displayOrder: number;
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
