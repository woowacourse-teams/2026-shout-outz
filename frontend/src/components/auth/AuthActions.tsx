import { useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';

import { logoutMutation, sessionQuery } from '@/apis/session';
import { myProfileQuery, myProfileSummaryQuery } from '@/apis/user';
import { IconUser } from '@tabler/icons-react';
import { Dropdown } from '@/components/Dropdown';
import { Avatar } from '@/components/Avatar';
import { AuthSheet } from '@/components/auth/AuthSheet';
import { Button } from '@/components/Button';
import { NotificationBell } from '@/components/notifications/NotificationBell';
import { UserHandle } from '@/components/users/UserHandle';
import { UserAffiliation } from '@/components/users/UserAffiliation';
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

  if (session.isPending) return <HeaderProfileSkeleton />;

  if (authenticated || session.data?.status === 'SIGNUP_REQUIRED') {
    return (
      <div className="flex items-center gap-2">
        {authenticated && <NotificationBell />}
        <Dropdown
          aria-label="내 계정 메뉴"
          triggerClassName="h-10 gap-2 px-2"
          menuClassName="w-64"
          trigger={
            <>
              {profile.data ? (
                <span className="relative shrink-0">
                  <Avatar
                    size="md"
                    src={profile.data.avatarUrl}
                    name={profile.data.displayName}
                    alt=""
                  />
                </span>
              ) : authenticated && profile.isPending ? (
                <HeaderProfileSkeleton />
              ) : (
                <IconUser className="size-5" aria-hidden="true" />
              )}
            </>
          }
        >
          {profile.data && (
            <div className="mb-1 border-b border-gray-100 px-3 py-3">
              <p className="flex min-w-0 flex-wrap items-baseline gap-x-1.5 text-sm font-semibold break-words text-gray-900">
                <span>{profile.data.displayName}</span>
                <UserHandle handle={profile.data.handle} userType={fullProfile.data?.userType} />
              </p>
              {fullProfile.data && <UserAffiliation {...fullProfile.data} />}
              {!fullProfile.data && fullProfile.isPending && <AffiliationSkeleton />}
            </div>
          )}
          {authenticated && profile.data && (
            <Dropdown.Item
              onSelect={() =>
                void navigate({ to: '/users/$handle', params: { handle: profile.data!.handle } })
              }
            >
              마이페이지
            </Dropdown.Item>
          )}
          <Dropdown.Item disabled={logout.isPending} onSelect={() => logout.mutate()}>
            {logout.isPending ? '로그아웃 중…' : '로그아웃'}
          </Dropdown.Item>
        </Dropdown>
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

function HeaderProfileSkeleton() {
  return (
    <span
      role="status"
      aria-label="프로필 정보를 불러오는 중"
      className="inline-flex items-center gap-2"
    >
      <span aria-hidden="true" className="flex items-center gap-2 motion-safe:animate-pulse">
        <span className="size-8 shrink-0 rounded-full bg-gray-100" />
      </span>
    </span>
  );
}

function AffiliationSkeleton() {
  return (
    <span role="status" aria-label="소속 정보를 불러오는 중" className="flex h-5 items-center">
      <span aria-hidden="true" className="h-3 w-44 rounded bg-gray-100 motion-safe:animate-pulse" />
    </span>
  );
}
