import { queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import type {
  UserProfileSuccessResponse,
  UserProfileSummarySuccessResponse,
} from '@/api/generated/schema';

/** 마이페이지 조회(`GET /api/v1/users/me`)는 공개 프로필과 같은 `UserProfileSuccessResponse`를 준다. */
export async function fetchMyProfile(signal?: AbortSignal) {
  const response = await httpClient<UserProfileSuccessResponse>('/api/v1/users/me', {
    method: 'get',
    signal,
  });
  if (!response) throw new Error('프로필 응답이 비어 있습니다.');
  return response.data;
}

export const myProfileQuery = (userId: number) =>
  queryOptions({
    queryKey: ['my-profile', userId],
    queryFn: ({ signal }) => fetchMyProfile(signal),
  });

export async function fetchMyProfileSummary(signal?: AbortSignal) {
  const response = await httpClient<UserProfileSummarySuccessResponse>('/api/v1/users/me/summary', {
    method: 'get',
    signal,
  });
  if (!response) throw new Error('프로필 응답이 비어 있습니다.');
  return response.data;
}

export const myProfileSummaryQuery = (userId: number) =>
  queryOptions({
    queryKey: ['my-profile-summary', userId],
    queryFn: ({ signal }) => fetchMyProfileSummary(signal),
  });
