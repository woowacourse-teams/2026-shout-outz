import { queryOptions } from '@tanstack/react-query';
import { type UserFeedItem, type UserProfile, type UserProjectCard } from '@/types/user';

export async function fetchUserProfile(handle: string): Promise<UserProfile> {
  throw new Error('fetchUserProfile은 아직 구현되지 않았습니다.');
}

export const userProfileQueryOptions = (handle: string) =>
  queryOptions({
    queryKey: ['users', handle],
    queryFn: () => fetchUserProfile(handle),
  });

/** 프로필 프로젝트 탭. 첫 페이지만 조회한다(다음 커서는 아직 쓰지 않는다). */
export async function fetchUserProjects(handle: string): Promise<UserProjectCard[]> {
  throw new Error('fetchUserProjects는 아직 구현되지 않았습니다.');
}

export const userProjectsQueryOptions = (handle: string) =>
  queryOptions({
    queryKey: ['users', handle, 'projects'],
    queryFn: () => fetchUserProjects(handle),
  });

/** 프로필 피드 탭. 첫 페이지만 조회한다. */
export async function fetchUserFeeds(handle: string): Promise<UserFeedItem[]> {
  throw new Error('fetchUserFeeds는 아직 구현되지 않았습니다.');
}

export const userFeedsQueryOptions = (handle: string) =>
  queryOptions({
    queryKey: ['users', handle, 'feeds'],
    queryFn: () => fetchUserFeeds(handle),
  });
