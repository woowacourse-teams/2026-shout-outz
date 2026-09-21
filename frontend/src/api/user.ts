import { infiniteQueryOptions, queryOptions } from '@tanstack/react-query';
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
  const page = await fetchUserProjectsPage(handle);
  return page.data;
}

async function fetchUserProjectsPage(handle: string, cursor?: string, signal?: AbortSignal) {
  const path = `${USERS_PATH}/${handle}/projects`;

  const body = await httpClient<
    ApiSuccessBody<
      ProjectSummary[],
      { nextCursor: string | null; hasNext: boolean }
    >
  >(path, { method: 'get', signal, searchParams: cursor ? { cursor } : undefined });
  if (!body) throw new Error(`프로필 프로젝트 응답이 비어 있습니다: ${path}`);

  return { data: body.data, meta: body.meta ?? { nextCursor: null, hasNext: false } };
}

export const userProjectsQueryOptions = (handle: string) =>
  queryOptions({
    queryKey: ['users', handle, 'projects'],
    queryFn: () => fetchUserProjects(handle),
  });

export const userProjectsInfiniteQueryOptions = (handle: string) =>
  infiniteQueryOptions({
    queryKey: ['users', handle, 'projects'],
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam, signal }) => fetchUserProjectsPage(handle, pageParam, signal),
    getNextPageParam: (last) =>
      last.meta.hasNext && last.meta.nextCursor ? last.meta.nextCursor : undefined,
  });

/** 프로필 피드 탭. 첫 페이지만 조회한다. 응답은 피드 목록과 같은 모양이다. */
export async function fetchUserFeeds(handle: string): Promise<Feed[]> {
  const page = await fetchUserFeedsPage(handle);
  return page.data;
}

async function fetchUserFeedsPage(handle: string, cursor?: string, signal?: AbortSignal) {
  const path = `${USERS_PATH}/${handle}/feeds`;

  const body = await httpClient<ApiSuccessBody<Feed[], { nextCursor: string | null; hasNext: boolean }>>(
    path,
    { method: 'get', signal, searchParams: cursor ? { cursor } : undefined },
  );
  if (!body) throw new Error(`프로필 피드 응답이 비어 있습니다: ${path}`);

  return { data: body.data, meta: body.meta ?? { nextCursor: null, hasNext: false } };
}

export const userFeedsQueryOptions = (handle: string) =>
  queryOptions({
    queryKey: ['users', handle, 'feeds'],
    queryFn: () => fetchUserFeeds(handle),
  });

export const userFeedsInfiniteQueryOptions = (handle: string) =>
  infiniteQueryOptions({
    queryKey: ['users', handle, 'feeds'],
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam, signal }) => fetchUserFeedsPage(handle, pageParam, signal),
    getNextPageParam: (last) =>
      last.meta.hasNext && last.meta.nextCursor ? last.meta.nextCursor : undefined,
  });
