import { render, screen } from '@testing-library/react';
import {
  createMemoryHistory,
  createRootRoute,
  createRoute,
  createRouter,
  RouterProvider,
} from '@tanstack/react-router';

import { HeroBanner } from '@/components/home/HeroBanner';
import { type HomeBanner } from '@/types/home';

const ROUTE_PATHS = ['/', '/projects/$id', '/news/$newsId', '/feeds/$feedId'];

const banner = (overrides: Partial<HomeBanner> = {}): HomeBanner => ({
  bannerId: 100,
  imageUrl: 'https://cdn.example.com/banners/loop.webp',
  destinationType: 'URL',
  ...overrides,
});

/** HeroBanner는 내부 이동에 라우터 Link를 쓰므로 라우터 안에서 그린다. */
const renderBanner = async (value: HomeBanner) => {
  const rootRoute = createRootRoute({ component: () => <HeroBanner banner={value} /> });
  const router = createRouter({
    routeTree: rootRoute.addChildren(
      ROUTE_PATHS.map((path) =>
        createRoute({ getParentRoute: () => rootRoute, path, component: () => null }),
      ),
    ),
    history: createMemoryHistory({ initialEntries: ['/'] }),
    scrollRestoration: false,
  });

  render(<RouterProvider router={router} />);
  await screen.findByRole('img');
};

describe('HeroBanner', () => {
  it('배너 이미지를 보여준다', async () => {
    await renderBanner(banner({ linkUrl: 'https://example.com' }));

    expect(screen.getByRole('img', { name: '홈 배너' })).toHaveAttribute(
      'src',
      'https://cdn.example.com/banners/loop.webp',
    );
  });

  it('EXTERNAL_URL이면 새 탭으로 여는 외부 링크다', async () => {
    await renderBanner(
      banner({ linkType: 'EXTERNAL_URL', linkUrl: 'https://example.com/promotion' }),
    );

    const link = screen.getByRole('link', { name: '홈 배너' });

    expect(link).toHaveAttribute('href', 'https://example.com/promotion');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', expect.stringContaining('noopener'));
  });

  it('INTERNAL_PATH면 새 탭을 열지 않는 내부 링크다', async () => {
    await renderBanner(banner({ linkType: 'INTERNAL_PATH', linkUrl: '/projects/3001' }));

    const link = screen.getByRole('link', { name: '홈 배너' });

    expect(link).toHaveAttribute('href', '/projects/3001');
    expect(link).not.toHaveAttribute('target');
  });

  it('TARGET이면 대상 리소스의 상세 경로로 가는 내부 링크다', async () => {
    await renderBanner(
      banner({ destinationType: 'TARGET', targetType: 'NEWS', targetId: 7, linkUrl: null }),
    );

    const link = screen.getByRole('link', { name: '홈 배너' });

    expect(link).toHaveAttribute('href', '/news/7');
    expect(link).not.toHaveAttribute('target');
  });

  it('갈 곳을 정할 수 없으면 링크 없이 이미지만 보여준다', async () => {
    await renderBanner(banner({ linkUrl: null }));

    expect(screen.getByRole('img', { name: '홈 배너' })).toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });
});
