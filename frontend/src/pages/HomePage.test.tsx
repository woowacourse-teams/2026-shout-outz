/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderRoute, server } from '@/test/renderRoute';

// httpClient(ky)가 5xx GET을 두 번 재시도한 뒤에 실패하므로 오류 화면은 기본 대기 시간보다 늦게 뜬다.
const ERROR_TIMEOUT = { timeout: 3000 };

const feedRegion = () => screen.getByRole('region', { name: '피드' });
const eventRegion = () => screen.getByRole('region', { name: '진행 중인 크루 이벤트' });

const findStatistic = async (label: string) => {
  const items = await within(
    await screen.findByRole('region', { name: '서비스 통계' }),
  ).findAllByRole('listitem');
  const item = items.find((element) => element.textContent?.includes(label));
  if (!item) throw new Error(`통계 항목을 찾을 수 없습니다: ${label}`);

  return item;
};

const findFeeds = async () =>
  within(await screen.findByRole('region', { name: '피드' })).findAllByRole('article');

const findEvents = async () =>
  within(await screen.findByRole('region', { name: '진행 중인 크루 이벤트' })).findAllByRole(
    'article',
  );

const failWith500 = (path: string) =>
  server.use(http.get(path, () => new HttpResponse(null, { status: 500 })));

const silenceConsoleError = () => jest.spyOn(console, 'error').mockImplementation(() => {});

describe('HomePage', () => {
  it('서비스 레이아웃 안에서 홈 탭을 현재 페이지로 표시한다', async () => {
    renderRoute('/');

    const header = await screen.findByRole('banner');
    expect(within(header).getByRole('link', { name: '홈' })).toHaveAttribute(
      'aria-current',
      'page',
    );
    expect(screen.getByRole('contentinfo')).toBeInTheDocument();
  });

  describe('서비스 통계', () => {
    it('프로젝트·피드·기수·진행 중 이벤트 수를 각 설명과 함께 보여준다', async () => {
      renderRoute('/');

      expect(await findStatistic('아카이빙된 프로젝트')).toHaveTextContent('128+');
      expect(await findStatistic('기술 글 · 회고 링크')).toHaveTextContent('341개');
      expect(await findStatistic('우아한테크코스 기수')).toHaveTextContent('8기수');
      expect(await findStatistic('진행 중 크루 이벤트')).toHaveTextContent('2건');
    });
  });

  describe('피드', () => {
    it('처음에는 최신순으로 3개를 보여준다', async () => {
      renderRoute('/');

      const feeds = await findFeeds();

      expect(feeds).toHaveLength(3);
      expect(within(feedRegion()).getByRole('tab', { name: '최신순' })).toHaveAttribute(
        'aria-selected',
        'true',
      );
      expect(feeds[0]).toHaveTextContent('루프 프로젝트에서 WebSocket');
      expect(feeds[1]).toHaveTextContent('TanStack Query v5의 낙관적 업데이트');
      expect(feeds[2]).toHaveTextContent('Server-Sent Events(SSE)');
    });

    it('인기순을 고르면 인기순으로 3개를 다시 보여준다', async () => {
      const user = userEvent.setup();
      renderRoute('/');
      await findFeeds();

      await user.click(within(feedRegion()).getByRole('tab', { name: '인기순' }));

      await waitFor(() =>
        expect(within(feedRegion()).getAllByRole('article')[0]).toHaveTextContent(
          '웹 접근성 스터디 4주 차 회고',
        ),
      );
      const feeds = within(feedRegion()).getAllByRole('article');
      expect(feeds).toHaveLength(3);
      expect(feeds[1]).toHaveTextContent('루프 프로젝트에서 WebSocket');
      expect(feeds[2]).toHaveTextContent('TanStack Query v5의 낙관적 업데이트');
      expect(within(feedRegion()).getByRole('tab', { name: '인기순' })).toHaveAttribute(
        'aria-selected',
        'true',
      );
    });

    it('피드 전체보기로 피드 페이지에 갈 수 있다', async () => {
      renderRoute('/');
      await findFeeds();

      expect(within(feedRegion()).getByRole('link', { name: /^피드 전체보기/ })).toHaveAttribute(
        'href',
        '/feeds',
      );
    });

    it('피드가 없으면 안내 문구를 보여준다', async () => {
      server.use(
        http.get('/api/v1/feeds', () =>
          HttpResponse.json({
            status: 'success',
            data: [],
            meta: { nextCursor: null, hasNext: false },
          }),
        ),
      );
      renderRoute('/');

      expect(await screen.findByText('아직 작성된 피드가 없습니다.')).toBeInTheDocument();
      expect(within(feedRegion()).queryByRole('article')).not.toBeInTheDocument();
    });
  });

  describe('진행 중인 크루 이벤트', () => {
    it('종료된 이벤트는 빼고 진행 중인 이벤트만 보여준다', async () => {
      renderRoute('/');

      const events = await findEvents();

      expect(events).toHaveLength(2);
      expect(events[0]).toHaveTextContent('6기 프로젝트 아카이빙 챌린지');
      expect(events[1]).toHaveTextContent('주간 베스트 기술 회고 피드 선정');
      expect(within(eventRegion()).queryByText(/\[종료\]/)).not.toBeInTheDocument();
    });

    it('소식 더보기로 소식 페이지에 갈 수 있다', async () => {
      renderRoute('/');
      await findEvents();

      expect(within(eventRegion()).getByRole('link', { name: /^소식 더보기/ })).toHaveAttribute(
        'href',
        '/news',
      );
    });

    it('진행 중인 이벤트가 없으면 안내 문구를 보여준다', async () => {
      server.use(
        http.get('/api/v1/news', () =>
          HttpResponse.json({ status: 'success', data: [], meta: { nextCursor: null } }),
        ),
      );
      renderRoute('/');

      expect(await screen.findByText('진행 중인 이벤트가 없습니다.')).toBeInTheDocument();
      expect(within(eventRegion()).queryByRole('article')).not.toBeInTheDocument();
    });
  });

  it.each([
    {
      path: '/api/v1/home/statistics',
      failedRegion: '서비스 통계',
      preservedRegions: ['피드', '진행 중인 크루 이벤트'],
    },
    {
      path: '/api/v1/feeds',
      failedRegion: '피드',
      preservedRegions: ['서비스 통계', '진행 중인 크루 이벤트'],
    },
    {
      path: '/api/v1/news',
      failedRegion: '진행 중인 크루 이벤트',
      preservedRegions: ['서비스 통계', '피드'],
    },
  ])(
    '$path 조회가 실패하면 해당 영역에만 에러 화면을 보여준다',
    async ({ path, failedRegion, preservedRegions }) => {
      failWith500(path);
      const consoleError = silenceConsoleError();
      try {
        renderRoute('/');

        expect(await screen.findByRole('alert', {}, ERROR_TIMEOUT)).toHaveTextContent(
          '요청에 실패했습니다. 다시 시도해 주세요.',
        );
        expect(screen.queryByRole('region', { name: failedRegion })).not.toBeInTheDocument();
        for (const region of preservedRegions) {
          expect(screen.getByRole('region', { name: region })).toBeInTheDocument();
        }
      } finally {
        consoleError.mockRestore();
      }
    },
  );
});
