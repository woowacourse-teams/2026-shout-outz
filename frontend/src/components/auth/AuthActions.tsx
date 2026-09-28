import { useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from '@tanstack/react-router';

import { logoutMutation, sessionQuery } from '@/apis/session';
import { myProfileSummaryQuery } from '@/apis/user';
import { Avatar } from '@/components/Avatar';
import { AuthSheet } from '@/components/auth/AuthSheet';
import { Button } from '@/components/Button';
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
        {/*
          누구인지 알기 전에는 링크를 내지 않는다. handle 없이 만들 수 있는 주소는 `/users`뿐인데,
          그쪽은 같은 요약을 한 번 더 조회해 리다이렉트하고, 실패하면 로그인 안내를 띄운다.
        */}
        {authenticated && profile.data && (
          <Link
            to="/users/$handle"
            params={{ handle: profile.data.handle }}
            className="flex items-center gap-2 text-sm font-medium text-gray-700 hover:text-gray-900"
          >
            <Avatar size="md" src={profile.data.avatarUrl} name={profile.data.displayName} alt="" />
            {/* 좁은 화면에서는 이름을 감추되 DOM에는 남긴다. 아바타만 남으면 링크에 읽을 이름이 없다. */}
            <span className="sr-only sm:not-sr-only">{profile.data.displayName}</span>
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
