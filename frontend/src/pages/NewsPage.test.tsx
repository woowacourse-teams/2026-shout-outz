/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderRoute, server } from '@/test/renderRoute';

const tabNamed = (name: string) => screen.getByRole('tab', { name });
const findNews = () => screen.findAllByRole('listitem');
const newsCount = async () => (await findNews()).length;

describe('NewsPage', () => {
  it('등록된 소식이 없으면 빈 상태를 보여준다', async () => {
    server.use(
      http.get('/api/v1/news', () =>
        HttpResponse.json({
          status: 'success',
          data: [],
          meta: { nextCursor: null, hasNext: false },
        }),
      ),
    );

    renderRoute('/news');

    expect(await screen.findByText('등록된 소식이 없습니다.')).toBeInTheDocument();
    expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '소식 더 보기' })).not.toBeInTheDocument();
  });

  it('조회 실패 후 다시 시도하면 소식 목록을 보여준다', async () => {
    server.use(http.get('/api/v1/news', () => new HttpResponse(null, { status: 500 })));
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});

    try {
      renderRoute('/news');
      const retry = await screen.findByRole('button', { name: '다시 시도' }, { timeout: 3000 });

      expect(screen.getByRole('alert')).toHaveTextContent('소식을 불러오지 못했습니다.');
      expect(screen.getByRole('banner', { name: '주요 헤더' })).toBeInTheDocument();
      expect(screen.getByRole('contentinfo')).toBeInTheDocument();

      server.resetHandlers();
      await userEvent.click(retry);

      expect(await newsCount()).toBe(4);
    } finally {
      consoleError.mockRestore();
    }
  });

  describe('URL에서 읽기', () => {
    it('파라미터가 없으면 전체를 보여준다', async () => {
      renderRoute('/news');

      expect(await newsCount()).toBe(4);
      expect(tabNamed('전체')).toHaveAttribute('aria-selected', 'true');
      expect(screen.getAllByRole('banner')).toHaveLength(1);
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

  it('목록의 소식을 누르면 해당 상세 페이지로 이동한다', async () => {
    const user = userEvent.setup();
    const router = renderRoute('/news');

    await user.click(
      await screen.findByRole('link', {
        name: /우아한테크코스 6기 최종 프로젝트 데모데이 일정 및 참관 안내/,
      }),
    );

    expect(router.state.location.pathname).toBe('/news/1');
    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: '우아한테크코스 6기 최종 프로젝트 데모데이 일정 및 참관 안내',
      }),
    ).toBeInTheDocument();
  });
});
