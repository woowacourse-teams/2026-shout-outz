import { mutationOptions, queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import type {
  UserVerificationRequestCreateSuccessResponse,
  UserVerificationRequestSuccessResponse,
} from '@/api/generated/schema';
import type { VerificationRequestBody, VerificationRequestData } from '@/types/api';
import type { Track } from '@/types/user';

/** 최신 인증 신청. 신청 이력이 없으면 서버가 data를 비워 보낸다. */
export type VerificationRequest = VerificationRequestData;

export type VerificationStatus = VerificationRequest['status'];

/** 신청 유형. 스키마는 GENERAL도 포함하지만 서버가 받는 값은 이 둘뿐이다. */
export type VerificationUserType = Extract<
  VerificationRequestBody['userType'],
  'WOOWACOURSE_CREW' | 'WOOWACOURSE_COACH'
>;

export type VerificationTrack = Track;

export interface VerificationRequestInput extends VerificationRequestBody {
  userType: VerificationUserType;
}

export async function fetchVerificationRequest(signal?: AbortSignal) {
  const response = await httpClient<UserVerificationRequestSuccessResponse>(
    '/api/v1/users/me/verification-request',
    { method: 'get', signal },
  );
  if (!response) throw new Error('구성원 인증 상태를 확인하지 못했습니다.');
  return response.data ?? null;
}

export const verificationRequestQuery = queryOptions({
  queryKey: ['verification-request'],
  queryFn: ({ signal }) => fetchVerificationRequest(signal),
  retry: false,
});

export async function createVerificationRequest(
  input: VerificationRequestInput,
): Promise<VerificationRequest> {
  const response = await httpClient<UserVerificationRequestCreateSuccessResponse>(
    '/api/v1/users/me/verification-requests',
    { method: 'post', json: input },
  );
  if (!response) throw new Error('구성원 인증 신청 결과를 확인하지 못했습니다.');

  // 생성 응답에는 decidedAt·reason이 없다. 조회 응답과 같은 모양으로 맞춰 캐시에 넣는다.
  return { ...response.data, decidedAt: null, reason: null };
}

export const createVerificationRequestMutation = mutationOptions({
  mutationFn: createVerificationRequest,
  retry: false,
});
