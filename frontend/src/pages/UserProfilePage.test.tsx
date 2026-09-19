/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderRoute, server } from '@/test/renderRoute';

const tabNamed = (name: string) => screen.getByRole('tab', { name });

const findProjects = async () =>
  within(await screen.findByRole('region', { name: '프로젝트' })).findAllByRole('article');

const findFeeds = async () =>
  within(await screen.findByRole('region', { name: '피드' })).findAllByRole('article');

describe('UserProfilePage', () => {
  it('handle에 해당하는 사람의 프로필을 보여준다', async () => {
    renderRoute('/users/woojin');

    expect(await screen.findByRole('heading', { name: '정우진' })).toBeInTheDocument();
    expect(screen.getByText('6기 백엔드')).toBeInTheDocument();
    expect(
      screen.getByText('대규모 트래픽 분산 처리와 데이터 정합성에 집착하는 백엔드 개발자입니다.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'GitHub' })).toHaveAttribute(
      'href',
      'https://github.com/woojin-dev',
    );
    expect(await screen.findByRole('link', { name: '구성원 인증' })).toHaveAttribute(
      'href',
      '/mypage/verification',
    );
  });

  describe('탭', () => {
    it('탭 이름에 프로필의 개수를 함께 보여준다', async () => {
      renderRoute('/users/woojin');

      expect(await screen.findByRole('tab', { name: '프로젝트 (2)' })).toBeInTheDocument();
      expect(tabNamed('피드 (18)')).toBeInTheDocument();
    });

    it('파라미터가 없으면 프로젝트 탭을 보여준다', async () => {
      renderRoute('/users/woojin');

      const projects = await findProjects();

      expect(projects).toHaveLength(2);
      expect(projects[0]).toHaveTextContent('모아모아 (MoaMoa)');
      expect(tabNamed('프로젝트 (2)')).toHaveAttribute('aria-selected', 'true');
    });

    it('tab으로 들어오면 그 탭을 보여준다', async () => {
      renderRoute('/users/woojin?tab=feeds');

      const feeds = await findFeeds();

      expect(feeds).toHaveLength(2);
      expect(feeds[0]).toHaveTextContent('영수증 OCR 파싱');
      expect(tabNamed('피드 (18)')).toHaveAttribute('aria-selected', 'true');
    });

    it('모르는 tab은 무시하고 프로젝트 탭으로 되돌린다', async () => {
      renderRoute('/users/woojin?tab=GARBAGE');

      expect(await findProjects()).toHaveLength(2);
      expect(tabNamed('프로젝트 (2)')).toHaveAttribute('aria-selected', 'true');
    });

    it('탭을 고르면 URL에 남아 공유와 뒤로가기가 가능하다', async () => {
      const user = userEvent.setup();
      const router = renderRoute('/users/woojin');
      await findProjects();

      await user.click(tabNamed('피드 (18)'));

      expect(router.state.location.searchStr).toBe('?tab=feeds');
      await waitFor(async () => expect(await findFeeds()).toHaveLength(2));
    });
  });

  describe('빈 목록', () => {
    it('프로젝트가 없으면 안내 문구를 보여준다', async () => {
      server.use(
        http.get('/api/v1/users/:handle/projects', () =>
          HttpResponse.json({
            status: 'success',
            data: [],
            meta: { nextCursor: null, hasNext: false },
          }),
        ),
      );
      renderRoute('/users/woojin');

      expect(await screen.findByText('등록한 프로젝트가 없습니다.')).toBeInTheDocument();
    });

    it('피드가 없으면 안내 문구를 보여준다', async () => {
      server.use(
        http.get('/api/v1/users/:handle/feeds', () =>
          HttpResponse.json({
            status: 'success',
            data: [],
            meta: { nextCursor: null, hasNext: false },
          }),
        ),
      );
      renderRoute('/users/woojin?tab=feeds');

      expect(await screen.findByText('작성한 피드가 없습니다.')).toBeInTheDocument();
    });
  });

  it('없는 사람이면 서버가 404를 주고 에러 화면을 보여준다', async () => {
    renderRoute('/users/nobody');

    expect(await screen.findByText('프로필을 불러오지 못했습니다.')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: '정우진' })).not.toBeInTheDocument();
  });
});
