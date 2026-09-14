import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';

import { routeTree } from '@/routeTree.gen';

const tabNamed = (name: string) => screen.getByRole('tab', { name });
const visibleNews = () => screen.getAllByRole('listitem');

const renderNewsRoute = async (entry: string) => {
  const router = createRouter({
    routeTree,
    history: createMemoryHistory({ initialEntries: [entry] }),
    scrollRestoration: false,
  });

  render(<RouterProvider router={router} />);
  await screen.findByRole('heading', { name: '소식', level: 1 });

  return router;
};

describe('NewsPage', () => {
  describe('URL에서 읽기', () => {
    it('파라미터가 없으면 전체를 보여준다', async () => {
      await renderNewsRoute('/news');

      expect(tabNamed('전체')).toHaveAttribute('aria-selected', 'true');
      expect(visibleNews()).toHaveLength(4);
    });

    it('type으로 들어오면 그 분류만 보여준다', async () => {
      await renderNewsRoute('/news?type=NOTICE');

      expect(tabNamed('공지사항')).toHaveAttribute('aria-selected', 'true');
      expect(visibleNews()).toHaveLength(1);
    });

    it('모르는 type은 무시하고 전체로 되돌린다', async () => {
      await renderNewsRoute('/news?type=GARBAGE');

      expect(tabNamed('전체')).toHaveAttribute('aria-selected', 'true');
      expect(visibleNews()).toHaveLength(4);
    });
  });

  describe('URL에 쓰기', () => {
    it('분류를 고르면 URL에 남아 공유와 뒤로가기가 가능하다', async () => {
      const user = userEvent.setup();
      const router = await renderNewsRoute('/news');

      await user.click(tabNamed('이벤트'));

      expect(router.state.location.searchStr).toBe('?type=EVENT');
      expect(visibleNews()).toHaveLength(3);
    });

    it('전체로 되돌리면 URL도 따라온다', async () => {
      const user = userEvent.setup();
      const router = await renderNewsRoute('/news?type=EVENT');

      await user.click(tabNamed('전체'));

      expect(router.state.location.searchStr).toBe('?type=ALL');
      expect(visibleNews()).toHaveLength(4);
    });
  });
});
