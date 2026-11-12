import { useEffect, useState, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from '@tanstack/react-router';

import { logoutMutation, sessionQuery } from '@/apis/session';
import { myProfileQuery, myProfileSummaryQuery } from '@/apis/user';
import { IconFileText, IconFolder, IconLogout, IconUser } from '@tabler/icons-react';
import { Dropdown } from '@/components/Dropdown';
import { Avatar } from '@/components/Avatar';
import { AuthSheet } from '@/components/auth/AuthSheet';
import { GithubMarkIcon } from '@/components/auth/GithubMarkIcon';
import { Button } from '@/components/Button';
import { NotificationBell } from '@/components/notifications/NotificationBell';
import { SIDE_DRAWER_ITEM, SideDrawerHeader } from '@/components/SideDrawerHeader';
import { Drawer, DrawerSideContent } from '@/components/ui/drawer';
import { UserHandle } from '@/components/users/UserHandle';
import { UserAffiliation } from '@/components/users/UserAffiliation';
import { useModal } from '@/hooks/useModal';
import { useSession } from '@/hooks/useSession';
import { getApiErrorMessage } from '@/utils/error';

export function AuthActions() {
  const { open } = useModal();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const session = useSession();
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
    if (session.data?.status === 'SIGNUP_REQUIRED')
      void navigate({
        to: '/signup',
        state: (previous) => ({ questionDraft: previous.questionDraft }),
      });
  }, [navigate, session.data?.status]);

  if (session.isPending) return <HeaderProfileSkeleton />;

  if (authenticated || session.data?.status === 'SIGNUP_REQUIRED') {
    const avatar = profile.data ? (
      <span className="relative shrink-0">
        <Avatar size="md" src={profile.data.avatarUrl} name={profile.data.displayName} alt="" />
      </span>
    ) : authenticated && profile.isPending ? (
      <HeaderProfileSkeleton />
    ) : (
      <IconUser className="size-5" aria-hidden="true" />
    );

    return (
      <div className="flex items-center gap-2">
        {authenticated && <NotificationBell />}
        {/* 모바일은 아바타를 누르면 오른쪽 "내 계정" 서랍을 연다. */}
        <MobileAccountDrawer
          trigger={avatar}
          profile={profile.data}
          loggingOut={logout.isPending}
          onLogout={() => logout.mutate()}
        />
        <div className="hidden md:block">
          <Dropdown
            aria-label="내 계정 메뉴"
            triggerClassName="rounded-full px-0"
            menuClassName="w-64"
            trigger={avatar}
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
        </div>
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
      variant="ghost"
      size="sm"
      className="gap-1.5 px-1 font-semibold text-gray-900"
      onClick={() => void open<void>((close) => <AuthSheet onClose={() => close()} />)}
    >
      <GithubMarkIcon className="size-4" />
      로그인
    </Button>
  );
}

interface MobileAccountDrawerProps {
  trigger: ReactNode;
  profile: { handle: string; displayName: string; avatarUrl?: string | null } | undefined;
  loggingOut: boolean;
  onLogout: () => void;
}

/** 모바일 내 계정 서랍. 프로필, 내 프로필·내가 쓴 질문 · 답변·내 프로젝트, 로그아웃 */
function MobileAccountDrawer({ trigger, profile, loggingOut, onLogout }: MobileAccountDrawerProps) {
  const [open, setOpen] = useState(false);
  const close = () => setOpen(false);
  const links = profile
    ? ([
        { label: '내 프로필', icon: IconUser, tab: undefined },
        { label: '내가 쓴 질문 · 답변', icon: IconFileText, tab: 'feeds' },
        { label: '내 프로젝트', icon: IconFolder, tab: 'projects' },
      ] as const)
    : [];

  return (
    <Drawer direction="right" open={open} onOpenChange={setOpen}>
      <button
        type="button"
        // 데스크톱 드롭다운 버튼(내 계정 메뉴)과 이름을 나눠, 화면 읽기 도구에서 둘을 구분한다.
        aria-label="내 계정 열기"
        aria-haspopup="dialog"
        aria-expanded={open}
        onClick={() => setOpen(true)}
        className="focus-visible:outline-primary-600 inline-flex cursor-pointer items-center rounded-full focus-visible:outline-2 md:hidden"
      >
        {/* 버튼 이름으로 충분해 안의 아바타·불러오는 중 표시는 읽지 않게 한다. */}
        <span aria-hidden="true">{trigger}</span>
      </button>
      <DrawerSideContent side="right" aria-describedby={undefined}>
        <SideDrawerHeader title="내 계정" />
        <div className="flex-1 overflow-auto pt-1 pb-4">
          {profile && (
            <>
              <div className="flex items-center gap-3 px-5 py-3.5">
                <Avatar
                  size="md"
                  className="size-10"
                  src={profile.avatarUrl}
                  name={profile.displayName}
                  alt=""
                />
                <span className="flex min-w-0 flex-col leading-snug">
                  <span className="truncate text-[0.9375rem] font-bold text-gray-900">
                    {profile.displayName}
                  </span>
                  <span className="truncate text-[0.8125rem] text-gray-500">@{profile.handle}</span>
                </span>
              </div>
              <div aria-hidden="true" className="mx-5 my-1.5 h-px bg-gray-100" />
              {links.map(({ label, icon: ItemIcon, tab }) => (
                <Link
                  key={label}
                  to="/users/$handle"
                  params={{ handle: profile.handle }}
                  search={tab ? { tab } : {}}
                  className={SIDE_DRAWER_ITEM}
                  onClick={close}
                >
                  <ItemIcon className="size-5 shrink-0" aria-hidden="true" />
                  {label}
                </Link>
              ))}
              <div aria-hidden="true" className="mx-5 my-1.5 h-px bg-gray-100" />
            </>
          )}
          <button
            type="button"
            disabled={loggingOut}
            onClick={() => {
              onLogout();
              close();
            }}
            className={`${SIDE_DRAWER_ITEM} py-2.5 text-sm text-gray-500 disabled:cursor-default`}
          >
            <span className="inline-flex w-5 justify-center text-gray-400">
              <IconLogout className="size-[17px]" aria-hidden="true" />
            </span>
            {loggingOut ? '로그아웃 중…' : '로그아웃'}
          </button>
        </div>
      </DrawerSideContent>
    </Drawer>
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
