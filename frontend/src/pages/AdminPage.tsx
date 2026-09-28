import type { ReactNode } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import { getRouteApi, Link } from '@tanstack/react-router';

import { sessionQuery } from '@/apis/session';
import { AppGnb } from '@/components/AppGnb';
import { getButtonStyles } from '@/components/Button';
import { Footer } from '@/components/Footer';
import { Tab } from '@/components/Tab';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { CrewApprovalPanel } from '@/components/admin/CrewApprovalPanel';
import { HomeBannerPanel } from '@/components/admin/HomeBannerPanel';
import { NewsCreatePanel } from '@/components/admin/NewsCreatePanel';
import { ProjectApprovalPanel } from '@/components/admin/ProjectApprovalPanel';
import { ADMIN_TAB_LABELS, ADMIN_TABS, DEFAULT_ADMIN_TAB } from '@/constants/admin';
import { type AdminTab } from '@/types/admin';
import { getGithubLoginUrl } from '@/utils/auth';

const route = getRouteApi('/admin');

/**
 * 관리자 페이지. 세션의 `role`이 `ADMIN`인 계정만 내용을 본다.
 *
 * 화면에서 막는 건 안내일 뿐이고, 실제 권한 검사는 서버가 관리자 API마다 403으로 한다.
 */
export function AdminPage() {
  const { data: session } = useSuspenseQuery(sessionQuery);

  if (session.status !== 'AUTHENTICATED') {
    return (
      <AdminGuard
        title="로그인이 필요해요."
        description="관리자 계정으로 로그인해 주세요."
        action={
          <a href={getGithubLoginUrl()} className={getButtonStyles({})}>
            GitHub 로그인
          </a>
        }
      />
    );
  }
  if (session.role !== 'ADMIN') {
    return (
      <AdminGuard
        title="접근 권한이 없어요."
        description="관리자 계정만 이용할 수 있는 페이지입니다."
        action={
          <Link to="/" className={getButtonStyles({})}>
            홈으로
          </Link>
        }
      />
    );
  }
  return <AdminContent />;
}

function AdminContent() {
  const { tab } = route.useSearch();
  const navigate = route.useNavigate();
  const currentTab = tab ?? DEFAULT_ADMIN_TAB;

  return (
    <AdminLayout>
      <main className="mx-auto w-full max-w-5xl flex-1 px-4 pt-5 pb-7 md:px-8 md:pt-10 md:pb-20">
        <h1 className="text-2xl font-bold">관리자</h1>
        <Tab
          value={currentTab}
          onChange={(next) => navigate({ search: { tab: next as AdminTab } })}
          aria-label="관리 메뉴"
          className="mt-6 w-full overflow-x-auto border-b border-gray-200"
        >
          {ADMIN_TABS.map((value) => (
            <Tab.Item key={value} value={value}>
              {ADMIN_TAB_LABELS[value]}
            </Tab.Item>
          ))}
        </Tab>
        <section className="mt-6">
          <AsyncBoundary>
            <AdminTabPanel tab={currentTab} />
          </AsyncBoundary>
        </section>
      </main>
    </AdminLayout>
  );
}

function AdminTabPanel({ tab }: { tab: AdminTab }) {
  switch (tab) {
    case 'crews':
      return <CrewApprovalPanel />;
    case 'projects':
      return <ProjectApprovalPanel />;
    case 'news':
      return <NewsCreatePanel />;
    case 'banners':
      return <HomeBannerPanel />;
  }
}

function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>관리자 | shout-outz</title>
      <AppGnb />
      {children}
      <Footer />
    </div>
  );
}

function AdminGuard({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action: ReactNode;
}) {
  return (
    <AdminLayout>
      <main className="mx-auto flex w-full max-w-xl flex-1 items-center px-4 py-12">
        <section className="w-full rounded-xl border border-gray-200 p-6 text-center md:p-8">
          <h1 className="text-xl font-bold">{title}</h1>
          <p className="mt-2 text-sm leading-relaxed text-gray-600">{description}</p>
          <div className="mt-6">{action}</div>
        </section>
      </main>
    </AdminLayout>
  );
}
