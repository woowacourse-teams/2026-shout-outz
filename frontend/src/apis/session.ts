import { queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import { setCsrfToken } from '@/utils/http';
export interface Session {
  status: 'AUTHENTICATED' | 'UNAUTHENTICATED' | 'SIGNUP_REQUIRED';
  userId: number | null;
  csrfToken: string;
  role: string | null;
}

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
