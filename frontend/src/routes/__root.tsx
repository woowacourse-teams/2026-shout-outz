import { createRootRoute, Outlet } from '@tanstack/react-router';
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools';

import { AnalyticsIdentifier } from '@/components/AnalyticsIdentifier';

// html/head/body는 Document(src/Document.tsx)의 책임입니다. 이 라우트는 라우팅 레이아웃
// 경계만 담당하고, 페이지별 레이아웃은 각 Page 컴포넌트에서 구성합니다.
const RootLayout = () => (
  <>
    <AnalyticsIdentifier />
    <Outlet />
    <TanStackRouterDevtools />
  </>
);

export const Route = createRootRoute({ component: RootLayout });
