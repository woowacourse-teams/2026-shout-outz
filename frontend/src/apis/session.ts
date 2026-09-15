import { queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import { setCsrfToken } from '@/utils/http';
export interface Session {
  status: 'AUTHENTICATED' | 'UNAUTHENTICATED' | 'SIGNUP_REQUIRED';
  userId: number | null;
  csrfToken: string;
  role: string | null;
}
export const sessionQuery = queryOptions({
  queryKey: ['auth-session'],
  staleTime: 0,
  retry: false,
  queryFn: async () => {
    try {
      const response = await httpClient<{ status: 'success'; data: Session }>(
        '/api/v1/auth/session',
        { method: 'get' },
      );
      if (!response) throw new Error('로그인 정보를 확인하지 못했습니다.');
      setCsrfToken(response.data.csrfToken);
      return response.data;
    } catch (error) {
      setCsrfToken(null);
      throw error;
    }
  },
});
