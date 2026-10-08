import { mutationOptions, queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import { setCsrfToken } from '@/utils/http';
import type { SessionData } from '@/types/api';
import type {
  OAuthSignupHandleAvailabilitySuccessResponse,
  OAuthSignupRequest,
  OAuthSignupSuccessResponse,
} from '@/api/generated/schema';

export type Session = SessionData;

export type SignupInput = OAuthSignupRequest;

export async function fetchSession(signal?: AbortSignal) {
  const response = await httpClient<{ status: 'success'; data: Session }>('/api/v1/auth/session', {
    method: 'get',
    signal,
  });

  if (!response) throw new Error('로그인 정보를 확인하지 못했습니다.');
  return response.data;
}

export const sessionQuery = queryOptions({
  queryKey: ['auth-session'],
  staleTime: 0,
  retry: false,
  queryFn: async ({ signal }) => {
    try {
      const session = await fetchSession(signal);
      setCsrfToken(session.csrfToken);
      return session;
    } catch (error) {
      setCsrfToken(null);
      throw error;
    }
  },
});

export async function signup(input: SignupInput) {
  const response = await httpClient<OAuthSignupSuccessResponse>('/api/v1/auth/signup', {
    method: 'post',
    json: input,
  });

  if (!response) throw new Error('가입 결과를 확인하지 못했습니다.');
  return response.data;
}

/**
 * 가입하려는 아이디(`@` 포함)를 쓸 수 있는지 확인한다.
 *
 * 서버는 GitHub 로그인 후 가입 전인 세션(`SIGNUP_REQUIRED`)에서만 응답하고, 그 밖에는 400을 준다.
 */
export async function fetchHandleAvailability(handle: string, signal?: AbortSignal) {
  const response = await httpClient<OAuthSignupHandleAvailabilitySuccessResponse>(
    '/api/v1/auth/signup/handle-availability',
    { method: 'get', searchParams: { handle }, signal },
  );

  if (!response) throw new Error('아이디 사용 가능 여부를 확인하지 못했습니다.');
  return response.data.available;
}

export const handleAvailabilityQuery = (handle: string) =>
  queryOptions({
    queryKey: ['handle-availability', handle],
    queryFn: ({ signal }) => fetchHandleAvailability(handle, signal),
    // 다른 사람이 먼저 가입할 수 있어 오래 믿지 않는다. 같은 값을 바로 다시 칠 때만 재사용한다.
    staleTime: 10_000,
    retry: false,
  });

export const signupMutation = mutationOptions({
  mutationFn: signup,
  retry: false,
});

export async function logout() {
  await httpClient('/api/v1/auth/logout', { method: 'post' });
  setCsrfToken(null);
}

export const logoutMutation = mutationOptions({
  mutationFn: logout,
  retry: false,
});
