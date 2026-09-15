/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { renderRoute } from '@/test/renderRoute';

const tabNamed = (name: string) => screen.getByRole('tab', { name });
const findNews = () => screen.findAllByRole('listitem');
const newsCount = async () => (await findNews()).length;

describe('NewsPage', () => {
  describe('URL에서 읽기', () => {
    it('파라미터가 없으면 전체를 보여준다', async () => {
      renderRoute('/news');

      expect(await newsCount()).toBe(4);
      expect(tabNamed('전체')).toHaveAttribute('aria-selected', 'true');
    });

    it('type으로 들어오면 그 분류만 보여준다', async () => {
      renderRoute('/news?type=NOTICE');

      expect(await newsCount()).toBe(1);
      expect(tabNamed('공지사항')).toHaveAttribute('aria-selected', 'true');
    });

    it('모르는 type은 무시하고 전체로 되돌린다', async () => {
      renderRoute('/news?type=GARBAGE');

      expect(await newsCount()).toBe(4);
      expect(tabNamed('전체')).toHaveAttribute('aria-selected', 'true');
    });
  });

  describe('URL에 쓰기', () => {
    it('분류를 고르면 URL에 남아 공유와 뒤로가기가 가능하다', async () => {
      const user = userEvent.setup();
      const router = renderRoute('/news');
      await findNews();

      await user.click(tabNamed('이벤트'));

      expect(router.state.location.searchStr).toBe('?type=EVENT');
      await waitFor(async () => expect(await newsCount()).toBe(3));
    });

    it('전체로 되돌리면 URL도 따라온다', async () => {
      const user = userEvent.setup();
      const router = renderRoute('/news?type=EVENT');
      await findNews();

      await user.click(tabNamed('전체'));

      expect(router.state.location.searchStr).toBe('?type=ALL');
      await waitFor(async () => expect(await newsCount()).toBe(4));
    });
  });
});
