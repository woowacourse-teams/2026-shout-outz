import { useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from '@tanstack/react-router';

import { logoutMutation, sessionQuery } from '@/apis/session';
import { myProfileQuery, myProfileSummaryQuery } from '@/apis/user';
import { Avatar } from '@/components/Avatar';
import { AuthSheet } from '@/components/auth/AuthSheet';
import { Button } from '@/components/Button';
import { CrewStatusBadge } from '@/components/users/CrewStatusBadge';
import { useModal } from '@/hooks/useModal';
import { getApiErrorMessage } from '@/utils/error';

export function AuthActions() {
  const { open } = useModal();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const session = useQuery({ ...sessionQuery, enabled: typeof window !== 'undefined' });
  const authenticated = session.data?.status === 'AUTHENTICATED' && session.data.userId !== null;
  const profile = useQuery({
    ...myProfileSummaryQuery(session.data?.userId ?? 0),
    enabled: authenticated,
  });
  const fullProfile = useQuery({
    ...myProfileQuery(session.data?.userId ?? 0),
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
        {/* 내 프로필 링크는 handle을 확인한 뒤에만 표시한다. */}
        {authenticated && profile.data && (
          <Link
            to="/users/$handle"
            params={{ handle: profile.data.handle }}
            className="flex items-center gap-2 text-sm font-medium text-gray-700 hover:text-gray-900"
          >
            <Avatar size="md" src={profile.data.avatarUrl} name={profile.data.displayName} alt="" />
            {/* 좁은 화면에서는 이름을 감추되 DOM에는 남긴다. 아바타만 남으면 링크에 읽을 이름이 없다. */}
            <span className="sr-only flex items-center gap-1 sm:not-sr-only">
              {profile.data.displayName}
              {fullProfile.data && (
                <CrewStatusBadge
                  userType={fullProfile.data.userType}
                  cohort={fullProfile.data.cohort}
                />
              )}
            </span>
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
    <Button
      size="sm"
      onClick={() => void open<void>((close) => <AuthSheet onClose={() => close()} />)}
    >
      로그인
    </Button>
  );
}
