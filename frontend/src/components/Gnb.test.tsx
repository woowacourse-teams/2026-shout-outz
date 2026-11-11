import { fireEvent, render, screen, within } from '@testing-library/react';
import {
  createMemoryHistory,
  createRootRoute,
  createRoute,
  createRouter,
  RouterProvider,
} from '@tanstack/react-router';

import { Gnb, GNB_ITEMS, type GnbProps } from '@/components/Gnb';

const ROUTE_PATHS = [
  '/',
  '/explore',
  '/community',
  '/community/$feedId',
  '/projects',
  '/projects/$projectId',
  '/job-postings/$jobPostingId',
  '/materials/$materialId',
  '/magazines',
  '/news',
];

const renderGnb = async (path: string, props: GnbProps = {}) => {
  const rootRoute = createRootRoute({ component: () => <Gnb {...props} /> });
  const router = createRouter({
    routeTree: rootRoute.addChildren(
      ROUTE_PATHS.map((routePath) =>
        createRoute({ getParentRoute: () => rootRoute, path: routePath, component: () => null }),
      ),
    ),
    history: createMemoryHistory({ initialEntries: [path] }),
    scrollRestoration: false,
  });

  render(<RouterProvider router={router} />);
  await screen.findByRole('navigation', { name: '서비스 메뉴' });
};

const itemNamed = (label: string) => screen.getByRole('link', { name: label });

/**
 * 로고도 `/`로 가는 링크라 홈에서는 활성으로 잡힌다. 여기서 확인하려는 것은
 * 네비게이션 항목 중 어느 것이 활성인지이므로 `<nav>` 안으로 범위를 좁힌다.
 */
const activeLabels = () =>
  within(screen.getByRole('navigation', { name: '서비스 메뉴' }))
    .getAllByRole('link')
    .filter((link) => link.getAttribute('aria-current') === 'page')
    .map((link) => link.textContent);

describe('Gnb', () => {
  describe('항목', () => {
    it('디자인의 네 항목을 링크로 렌더한다', async () => {
      await renderGnb('/');

      GNB_ITEMS.forEach((item) => {
        expect(itemNamed(item.label)).toHaveAttribute('href', item.to);
      });
    });
  });

  describe('활성 표시', () => {
    it('현재 경로에 해당하는 항목 하나만 활성이다', async () => {
      await renderGnb('/community');

      expect(activeLabels()).toEqual(['커뮤니티']);
    });

    it('홈에서는 활성 항목이 없다', async () => {
      await renderGnb('/');
      expect(activeLabels()).toEqual([]);
    });

    it('탐색 화면에서는 탐색이 활성이다', async () => {
      await renderGnb('/explore');
      expect(activeLabels()).toEqual(['탐색']);
    });

    it('하위 경로에서도 상위 항목이 활성이다', async () => {
      await renderGnb('/community/1');

      expect(activeLabels()).toEqual(['커뮤니티']);
    });

    it('프로젝트 목록에서는 탐색이 활성이다', async () => {
      await renderGnb('/projects');
      expect(activeLabels()).toEqual(['탐색']);
    });

    it('프로젝트 상세에서도 탐색이 활성이다', async () => {
      await renderGnb('/projects/1');
      expect(activeLabels()).toEqual(['탐색']);
    });

    it('채용공고 상세에서도 탐색이 활성이다', async () => {
      await renderGnb('/job-postings/1');
      expect(activeLabels()).toEqual(['탐색']);
    });
  });

  describe('구조', () => {
    it('네비게이션을 한 벌만 렌더한다', async () => {
      await renderGnb('/');

      expect(screen.getAllByRole('navigation', { name: '서비스 메뉴' })).toHaveLength(1);
    });

    it('모바일 ☰을 누르면 메뉴 서랍에 네 항목을 보여 주고 현재 항목을 표시한다', async () => {
      await renderGnb('/community');

      fireEvent.click(screen.getByRole('button', { name: '메뉴 열기' }));

      const drawer = await screen.findByRole('dialog', { name: '메뉴' });
      const links = within(drawer).getAllByRole('link');
      expect(links.map((link) => link.textContent)).toEqual(GNB_ITEMS.map((item) => item.label));
      expect(within(drawer).getByRole('link', { name: '커뮤니티' })).toHaveAttribute(
        'aria-current',
        'page',
      );
    });

    it('trailing에 넘긴 내용을 렌더한다', async () => {
      await renderGnb('/', { trailing: <span>정우진</span> });

      expect(screen.getByText('정우진')).toBeInTheDocument();
    });
  });
});
