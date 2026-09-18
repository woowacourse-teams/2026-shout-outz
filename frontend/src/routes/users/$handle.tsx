import { Suspense } from 'react';
import { createFileRoute } from '@tanstack/react-router';

import { UserProfilePage } from '@/pages/UserProfilePage';
import { isProfileTab, type ProfileTab } from '@/types/user';

interface ProfileSearch {
  tab?: ProfileTab;
}

export const Route = createFileRoute('/users/$handle')({
  validateSearch: (search: Record<string, unknown>): ProfileSearch => ({
    tab: isProfileTab(search.tab) ? search.tab : undefined,
  }),
  component: RouteComponent,
  errorComponent: ProfileError,
});

function RouteComponent() {
  return (
    <Suspense fallback={<ProfileMessage>프로필을 불러오는 중…</ProfileMessage>}>
      <UserProfilePage />
    </Suspense>
  );
}

function ProfileError() {
  return <ProfileMessage>프로필을 불러오지 못했습니다.</ProfileMessage>;
}

function ProfileMessage({ children }: { children: string }) {
  return (
    <main className="px-4 pt-5 pb-7 md:px-16 md:pt-10 md:pb-20">
      <p className="text-sm text-gray-600">{children}</p>
    </main>
  );
}
