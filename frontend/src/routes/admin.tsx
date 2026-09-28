import { Suspense } from 'react';
import { createFileRoute } from '@tanstack/react-router';

import { AdminPage } from '@/pages/AdminPage';
import { isAdminTab, type AdminTab } from '@/types/admin';

interface AdminSearch {
  tab?: AdminTab;
}

export const Route = createFileRoute('/admin')({
  validateSearch: (search: Record<string, unknown>): AdminSearch => ({
    tab: isAdminTab(search.tab) ? search.tab : undefined,
  }),
  component: RouteComponent,
  errorComponent: AdminError,
});

function RouteComponent() {
  return (
    <Suspense fallback={<AdminMessage>관리자 화면을 불러오는 중…</AdminMessage>}>
      <AdminPage />
    </Suspense>
  );
}

function AdminError() {
  return <AdminMessage>관리자 화면을 불러오지 못했습니다.</AdminMessage>;
}

function AdminMessage({ children }: { children: string }) {
  return (
    <main className="px-4 pt-5 pb-7 md:px-16 md:pt-10 md:pb-20">
      <p className="text-sm text-gray-600">{children}</p>
    </main>
  );
}
