import { queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';

interface ProfileSummary {
  handle: string;
  displayName: string;
  avatarImageId: number | null;
}

interface MyProfile extends ProfileSummary {
  userType: string;
  track: string | null;
  cohort: number | null;
}

export async function fetchMyProfile(signal?: AbortSignal) {
  const response = await httpClient<{ status: 'success'; data: MyProfile }>('/api/v1/users/me', {
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
  const response = await httpClient<{ status: 'success'; data: ProfileSummary }>(
    '/api/v1/users/me/summary',
    { method: 'get', signal },
  );
  if (!response) throw new Error('프로필 응답이 비어 있습니다.');
  return response.data;
}

export const myProfileSummaryQuery = (userId: number) =>
  queryOptions({
    queryKey: ['my-profile-summary', userId],
    queryFn: ({ signal }) => fetchMyProfileSummary(signal),
  });
