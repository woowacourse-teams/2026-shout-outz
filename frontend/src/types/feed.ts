import type {
  FeedCommentData,
  FeedData,
  FeedListItemData,
  Item,
  UserFeedListItemData,
} from '@/types/api';

/** GET /api/v1/feeds의 sort 쿼리 파라미터. 스키마가 쿼리 파라미터는 내보내지 않아 여기서 정의한다. */
export type FeedSort = 'LATEST' | 'POPULAR';

export interface FeedListParams {
  sort: FeedSort;
  size: number;
}

/**
 * 피드 한 건.
 *
 * 상세(`GET /feeds/{feedId}`)와 목록(`GET /feeds`)의 항목이 같은 모양이라 하나로 쓴다.
 * 프로필 피드 탭(`GET /users/{handle}/feeds`)만 `likeCount`·`commentCount`가 더 붙는데,
 * 구조적으로 이 타입에 대입되므로 카드 컴포넌트는 그대로 받을 수 있다.
 */
export type Feed = FeedData;
export type FeedListItem = FeedListItemData;
export type UserFeedListItem = UserFeedListItemData;

export type FeedAuthor = Feed['author'];
export type FeedCategory = Item<Feed['categories']>;
/** 조회 응답의 본문 미디어. 공개 URL만 내려오고 mediaId는 없다(작성 요청에만 쓴다). */
export type FeedMedia = Item<Feed['media']>;

export type FeedComment = FeedCommentData;
