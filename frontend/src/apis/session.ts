import { mutationOptions, queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import { setCsrfToken } from '@/utils/http';
import type { SessionData, SessionStatus } from '@/types/api';
import type { OAuthSignupRequest, OAuthSignupSuccessResponse } from '@/api/generated/schema';

export type Session = SessionData;
export type { SessionStatus };

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
