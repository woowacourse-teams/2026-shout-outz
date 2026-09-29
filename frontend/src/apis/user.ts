import { mutationOptions, queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import { isApiResponseError } from '@/utils/error';
import type {
  UserProfileSuccessResponse,
  UserProfileSummarySuccessResponse,
  UserProfileUpdateRequest,
  UserProfileUpdateSuccessResponse,
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

export type UpdateMyProfileInput = UserProfileUpdateRequest;

/**
 * 미디어 처리가 아직 안 끝났을 때 서버가 주는 코드.
 *
 * `POST /media/{id}/complete`가 200을 줘도 서버 안에서는 아직 READY가 아닐 수 있다.
 * 그 사이에 프로필을 저장하면 이 코드로 거절된다.
 */
const AVATAR_IMAGE_NOT_READY = 'AVATAR_IMAGE_NOT_READY';

/** 처리를 기다리는 간격. 다 쓰면 포기하고 오류를 그대로 올린다. */
const NOT_READY_RETRY_DELAYS_MS = [300, 600, 1200];

const isAvatarNotReady = (error: unknown) =>
  isApiResponseError(error) && error.data.code === AVATAR_IMAGE_NOT_READY;

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

async function putMyProfile(input: UpdateMyProfileInput) {
  const response = await httpClient<UserProfileUpdateSuccessResponse>('/api/v1/users/me', {
    method: 'put',
    json: input,
  });
  if (!response) throw new Error('프로필 수정 결과를 확인하지 못했습니다.');

  return response.data;
}

/**
 * 프로필을 저장한다.
 *
 * 사진을 막 올린 직후에는 서버가 아직 이미지를 처리하는 중일 수 있다. 그때만
 * (`AVATAR_IMAGE_NOT_READY`) 잠깐 기다렸다 다시 보낸다. 다른 오류는 바로 올린다.
 */
export async function updateMyProfile(input: UpdateMyProfileInput) {
  for (let attempt = 0; ; attempt += 1) {
    try {
      return await putMyProfile(input);
    } catch (error) {
      const delayMs = NOT_READY_RETRY_DELAYS_MS[attempt];
      if (delayMs === undefined || !isAvatarNotReady(error)) throw error;

      await sleep(delayMs);
    }
  }
}

export const updateMyProfileMutation = mutationOptions({
  mutationFn: updateMyProfile,
  retry: false,
});
