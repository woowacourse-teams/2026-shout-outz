import { useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from '@tanstack/react-router';

import { logoutMutation, sessionQuery } from '@/apis/session';
import { myProfileSummaryQuery } from '@/apis/user';
import { Button, getButtonStyles } from '@/components/Button';
import { getGithubLoginUrl } from '@/utils/auth';
import { getApiErrorMessage } from '@/utils/error';

export function AuthActions() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const session = useQuery({ ...sessionQuery, enabled: typeof window !== 'undefined' });
  const authenticated = session.data?.status === 'AUTHENTICATED' && session.data.userId !== null;
  const profile = useQuery({
    ...myProfileSummaryQuery(session.data?.userId ?? 0),
    enabled: authenticated,
  });
  const logout = useMutation({
    ...logoutMutation,
    onSuccess: async () => {
      queryClient.removeQueries({ queryKey: ['my-profile'] });
      queryClient.removeQueries({ queryKey: ['my-profile-summary'] });
      await queryClient.invalidateQueries({ queryKey: sessionQuery.queryKey });
    },
  });

  useEffect(() => {
    if (session.data?.status === 'SIGNUP_REQUIRED') void navigate({ to: '/signup' });
  }, [navigate, session.data?.status]);

  if (session.isPending) return null;

  if (authenticated || session.data?.status === 'SIGNUP_REQUIRED') {
    return (
      <div className="flex items-center gap-2">
        {authenticated && (
          <Link
            to="/users"
            className="hidden text-sm font-medium text-gray-700 hover:text-gray-900 sm:inline"
          >
            {profile.data?.displayName ?? '로그인됨'}
          </Link>
        )}
        <Button
          size="sm"
          variant="ghost"
          disabled={logout.isPending}
          onClick={() => logout.mutate()}
        >
          {logout.isPending ? '로그아웃 중…' : '로그아웃'}
        </Button>
        {logout.isError && (
          <span className="sr-only" role="alert">
            {getApiErrorMessage(logout.error)}
          </span>
        )}
      </div>
    );
  }

  return (
    <a href={getGithubLoginUrl()} className={getButtonStyles({ size: 'sm' })}>
      로그인
    </a>
  );
}
