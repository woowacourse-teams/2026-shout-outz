import { Suspense } from 'react';
import { createFileRoute } from '@tanstack/react-router';

import { AppGnb } from '@/components/AppGnb';
import { Footer } from '@/components/Footer';
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
});

function RouteComponent() {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <AppGnb />
      <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-4 pt-6 pb-12 md:gap-8 md:pt-10 md:pb-20">
        <Suspense fallback={<ProfilePageSkeleton />}>
          <UserProfilePage />
        </Suspense>
      </main>
      <Footer />
    </div>
  );
}

function ProfilePageSkeleton() {
  return (
    <div role="status" aria-label="프로필을 불러오는 중" className="animate-pulse space-y-8">
      <div className="flex items-center gap-4">
        <div className="size-20 shrink-0 rounded-full bg-gray-200" />
        <div className="flex-1 space-y-3">
          <div className="h-6 w-36 rounded bg-gray-200" />
          <div className="h-4 w-28 rounded bg-gray-200" />
        </div>
      </div>
      <div className="h-10 rounded-lg bg-gray-100" />
      <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
        {[0, 1].map((item) => (
          <div key={item} className="space-y-3">
            <div className="aspect-video rounded-xl bg-gray-200" />
            <div className="h-4 w-1/3 rounded bg-gray-200" />
            <div className="h-5 w-2/3 rounded bg-gray-200" />
            <div className="h-4 rounded bg-gray-100" />
          </div>
        ))}
      </div>
    </div>
  );
}
