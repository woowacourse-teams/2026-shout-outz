import { PROFILE_TABS } from '@/constants/user';
import type { Track, UserProfileData, UserSearchItem, UserType } from '@/types/api';

export type ProfileTab = (typeof PROFILE_TABS)[number];

// 사전에 정의한 값이 아닐 경우 undefined로 덮기 위한 용도의 타입 가드
export const isProfileTab = (value: unknown): value is ProfileTab =>
  PROFILE_TABS.some((tab) => tab === value);

export type { Track, UserType, UserSearchItem };

/** 공개 프로필. `GET /api/v1/users/{handle}` */
export type UserProfile = UserProfileData;
