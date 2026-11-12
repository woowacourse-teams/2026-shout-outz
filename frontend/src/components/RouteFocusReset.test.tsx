import { act, render, screen } from '@testing-library/react';
import {
  createMemoryHistory,
  createRootRoute,
  createRoute,
  createRouter,
  Outlet,
  RouterProvider,
} from '@tanstack/react-router';

import { RouteFocusReset } from '@/components/RouteFocusReset';

const renderRouter = async () => {
  const rootRoute = createRootRoute({
    component: () => (
      <>
        <RouteFocusReset />
        {/* 페이지가 바뀌어도 남아 있는 버튼이라, 초기화하지 않으면 포커스가 그대로 머문다. */}
        <button type="button">남아 있는 버튼</button>
        <dialog open>
          <button type="button">모달 안 버튼</button>
        </dialog>
        <Outlet />
      </>
    ),
  });
  const router = createRouter({
    routeTree: rootRoute.addChildren(
      ['/', '/about'].map((path) =>
        createRoute({
          getParentRoute: () => rootRoute,
          path,
          component: () => <main>{path}</main>,
        }),
      ),
    ),
    history: createMemoryHistory({ initialEntries: ['/'] }),
    scrollRestoration: false,
  });

  render(<RouterProvider router={router} />);
  await screen.findByRole('main');
  return router;
};

describe('RouteFocusReset', () => {
  it('첫 접속에서는 포커스를 옮기지 않는다', async () => {
    await renderRouter();

    expect(document.body).not.toHaveAttribute('tabindex');
  });

  it('다른 페이지로 이동하면 포커스를 문서의 처음으로 되돌린다', async () => {
    const router = await renderRouter();
    const button = screen.getByRole('button', { name: '남아 있는 버튼' });
    button.focus();

    await act(() => router.navigate({ to: '/about' }));

    expect(button).not.toHaveFocus();
    expect(document.body).toHaveFocus();
  });

  it('포커스가 문서의 처음을 벗어나면 body에 붙인 tabindex를 지운다', async () => {
    const router = await renderRouter();

    await act(() => router.navigate({ to: '/about' }));
    screen.getByRole('button', { name: '남아 있는 버튼' }).focus();

    expect(document.body).not.toHaveAttribute('tabindex');
  });

  it('검색 조건만 바뀌면 포커스를 그대로 둔다', async () => {
    const router = await renderRouter();
    const button = screen.getByRole('button', { name: '남아 있는 버튼' });
    button.focus();

    await act(() => router.navigate({ to: '/', search: { sort: 'POPULAR' } }));

    expect(button).toHaveFocus();
  });

  it('열려 있는 모달 안의 포커스는 그대로 둔다', async () => {
    const router = await renderRouter();
    const button = screen.getByRole('button', { name: '모달 안 버튼', hidden: true });
    button.focus();

    await act(() => router.navigate({ to: '/about' }));

    expect(button).toHaveFocus();
  });
});
