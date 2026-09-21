import { mutationOptions, queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';

export type VerificationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type VerificationUserType = 'WOOWACOURSE_CREW' | 'WOOWACOURSE_COACH';
export type VerificationTrack = 'BACKEND' | 'FRONTEND' | 'ANDROID';

export interface VerificationRequest {
  requestId: number | null;
  userType: VerificationUserType;
  nickname: string;
  cohort: number | null;
  track: VerificationTrack | null;
  status: VerificationStatus;
  requestedAt: string | null;
  decidedAt: string | null;
  reason: string | null;
}

export interface VerificationRequestInput {
  userType: VerificationUserType;
  nickname: string;
  cohort: number | null;
  track: VerificationTrack | null;
}

type CreatedVerificationRequest = Omit<VerificationRequest, 'decidedAt' | 'reason'>;

export async function fetchVerificationRequest(signal?: AbortSignal) {
  const response = await httpClient<{ status: 'success'; data: VerificationRequest | null }>(
    '/api/v1/users/me/verification-request',
    { method: 'get', signal },
  );
  if (!response) throw new Error('구성원 인증 상태를 확인하지 못했습니다.');
  return response.data;
}

export const verificationRequestQuery = queryOptions({
  queryKey: ['verification-request'],
  queryFn: ({ signal }) => fetchVerificationRequest(signal),
  retry: false,
});

export async function createVerificationRequest(input: VerificationRequestInput) {
  const response = await httpClient<{ status: 'success'; data: CreatedVerificationRequest }>(
    '/api/v1/users/me/verification-requests',
    { method: 'post', json: input },
  );
  if (!response) throw new Error('구성원 인증 신청 결과를 확인하지 못했습니다.');
  return { ...response.data, decidedAt: null, reason: null } satisfies VerificationRequest;
}

export const createVerificationRequestMutation = mutationOptions({
  mutationFn: createVerificationRequest,
  retry: false,
});
