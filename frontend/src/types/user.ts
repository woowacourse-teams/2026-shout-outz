import { PROFILE_TABS } from '@/constants/user';

export type ProfileTab = (typeof PROFILE_TABS)[number];

// 사전에 정의한 값이 아닐 경우 undefined로 덮기 위한 용도의 타입 가드
export const isProfileTab = (value: unknown): value is ProfileTab =>
  PROFILE_TABS.some((tab) => tab === value);

// TODO 대체 UserProfile
export interface UserProfile {
  userId: number;
  handle: string;
  displayName: string;
  userType: string;
  track: string | null;
  cohort: number | null;
  bio: string | null;
  avatarImageId: number | null;
  githubProfileUrl: string | null;
  blogUrl: string | null;
  counts: {
    projects: number;
    feeds: number;
  };
}

/**
 * 프로필 피드 탭 응답. feedId는 피드 목록(`Feed`)의 feedId와 같은 값이고,
 * 작성자 이미지가 avatarUrl, 미디어가 url이며 반응·댓글 수를 포함한다.
 */
// TODO 대체 UserFeed
export interface UserFeedItem {
  feedId: number;
  content: string;
  author: {
    userId: number;
    handle: string;
    displayName: string;
    userType: string;
    track: string | null;
    cohort: number | null;
    avatarUrl: string | null;
  };
  categories: { categoryId: number; slug: string; displayName: string }[];
  media: { url: string; displayOrder: number }[];
  reactionCounts: { LIKE: number };
  viewerReactionTypes: string[];
  commentCount: number;
  createdAt: string;
  updatedAt: string;
}
