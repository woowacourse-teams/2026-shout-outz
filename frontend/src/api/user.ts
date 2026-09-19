import { queryOptions } from '@tanstack/react-query';
import { type ProjectSummary } from '@/types/project';
import { type Feed } from '@/types/feed';
import { type UserProfile } from '@/types/user';
import { type ApiSuccessBody, httpClient } from '@/utils/client';

const USERS_PATH = '/api/v1/users';

export async function fetchUserProfile(handle: string): Promise<UserProfile> {
  const path = `${USERS_PATH}/${handle}`;

  const body = await httpClient<ApiSuccessBody<UserProfile>>(path, { method: 'get' });
  if (!body) throw new Error(`프로필 응답이 비어 있습니다: ${path}`);

  return body.data;
}

export const userProfileQueryOptions = (handle: string) =>
  queryOptions({
    queryKey: ['users', handle],
    queryFn: () => fetchUserProfile(handle),
  });

/** 프로필 프로젝트 탭. 첫 페이지만 조회한다(다음 커서는 아직 쓰지 않는다). */
export async function fetchUserProjects(handle: string): Promise<ProjectSummary[]> {
  const path = `${USERS_PATH}/${handle}/projects`;

  const body = await httpClient<ApiSuccessBody<ProjectSummary[]>>(path, { method: 'get' });
  if (!body) throw new Error(`프로필 프로젝트 응답이 비어 있습니다: ${path}`);

  return body.data;
}

export const userProjectsQueryOptions = (handle: string) =>
  queryOptions({
    queryKey: ['users', handle, 'projects'],
    queryFn: () => fetchUserProjects(handle),
  });

/** 프로필 피드 탭. 첫 페이지만 조회한다. 응답은 피드 목록과 같은 모양이다. */
export async function fetchUserFeeds(handle: string): Promise<Feed[]> {
  const path = `${USERS_PATH}/${handle}/feeds`;

  const body = await httpClient<ApiSuccessBody<Feed[]>>(path, { method: 'get' });
  if (!body) throw new Error(`프로필 피드 응답이 비어 있습니다: ${path}`);

  return body.data;
}

export const userFeedsQueryOptions = (handle: string) =>
  queryOptions({
    queryKey: ['users', handle, 'feeds'],
    queryFn: () => fetchUserFeeds(handle),
  });
