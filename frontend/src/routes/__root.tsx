import { createRootRoute, Link, Outlet } from '@tanstack/react-router';
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools';

// html/head/body는 Document(src/Document.tsx)의 책임입니다. 이 라우트는 라우팅 레이아웃
// (nav, Outlet)만 담당하고, TanStack Router의 head/meta API에는 의존하지 않습니다.
const RootLayout = () => (
    <>
        <div className="p-2 flex gap-2">
            <Link to="/" className="[&.active]:font-bold">
                Home
            </Link>
            <Link to="/about" className="[&.active]:font-bold">
                About
            </Link>
        </div>
        <hr />
        <Outlet />
        <TanStackRouterDevtools />
    </>
);

export const Route = createRootRoute({ component: RootLayout });
