import { PROFILE_TABS } from '@/constants/user';

export type ProfileTab = (typeof PROFILE_TABS)[number];

// 사전에 정의한 값이 아닐 경우 undefined로 덮기 위한 용도의 타입 가드
export const isProfileTab = (value: unknown): value is ProfileTab =>
  PROFILE_TABS.some((tab) => tab === value);

// TODO 대체 UserProfile
export interface UserProfile {
  handle: string;
  displayName: string;
  userType: 'GENERAL' | 'WOOWACOURSE_CREW' | 'WOOWACOURSE_COACH';
  track: 'BACKEND' | 'ANDROID' | 'FRONTEND' | null;
  cohort: number | null;
  bio: string | null;
  avatarUrl: string | null;
  githubProfileUrl: string | null;
  blogUrl: string | null;
  counts: {
    projects: number;
    feeds: number;
  };
}
