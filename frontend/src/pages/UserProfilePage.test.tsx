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
    expect(screen.getByText('8기 백엔드')).toBeInTheDocument();
    expect(
      screen.getByText('대규모 트래픽 분산 처리와 데이터 정합성에 집착하는 백엔드 개발자입니다.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'GitHub' })).toHaveAttribute(
      'href',
      'https://github.com/woojin-dev',
    );
    expect(screen.queryByRole('link', { name: '구성원 인증' })).not.toBeInTheDocument();
  });

  describe('탭', () => {
    it('탭 이름에 프로필의 개수를 함께 보여준다', async () => {
      renderRoute('/users/woojin');

      expect(await screen.findByRole('tab', { name: '프로젝트 (3)' })).toBeInTheDocument();
      expect(tabNamed('피드 (18)')).toBeInTheDocument();
    });

    it('파라미터가 없으면 프로젝트 탭을 보여준다', async () => {
      renderRoute('/users/woojin');

      const projects = await findProjects();

      expect(projects).toHaveLength(3);
      expect(projects[0]).toHaveTextContent('모아모아 (MoaMoa)');
      expect(projects[2]).toHaveTextContent('스터디 메이트');
      expect(projects[2]!.closest('a')).toHaveAttribute('href', '/projects/@study-mate');
      expect(tabNamed('프로젝트 (3)')).toHaveAttribute('aria-selected', 'true');

      expect(within(projects[2]!).getByRole('status')).toHaveTextContent('승인 반려');
      expect(projects[2]).toHaveTextContent(
        '프로젝트 소개에 해결하려는 문제와 핵심 기능을 구체적으로 적어 주세요.',
      );
      expect(projects[2]).not.toHaveTextContent('반려 사유:');
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

      expect(await findProjects()).toHaveLength(3);
      expect(tabNamed('프로젝트 (3)')).toHaveAttribute('aria-selected', 'true');
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

describe('프로필 사진', () => {
  const pickFile = async (user: ReturnType<typeof userEvent.setup>, label: string) => {
    const file = new File(['x'], 'me.png', { type: 'image/png' });
    // 버튼은 숨겨진 file input을 대신 눌러 주는 것이라, 테스트는 input에 직접 올린다.
    const input = screen.getByLabelText(label);
    await user.upload(input, file);
  };

  it('내 프로필에서는 사진을 바로 올릴 수 있다', async () => {
    const user = userEvent.setup();
    let body: unknown;
    server.use(
      http.put('/api/v1/users/me', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({
          status: 'success',
          data: { handle: 'woojin', displayName: '정우진', userType: 'WOOWACOURSE_CREW' },
        });
      }),
    );

    renderRoute('/users/woojin');
    await screen.findByRole('button', { name: '프로필 사진 추가' });

    await pickFile(user, '프로필 사진 추가');

    // 올린 mediaId를 avatarImageId로 싣고, 나머지 값은 그대로 다시 보낸다.
    await waitFor(() => expect(body).toHaveProperty('avatarImageId', 12));
    expect(body).toHaveProperty('displayName', '정우진');
    expect(body).toHaveProperty('bio');
  });

  it('남의 프로필에는 사진 버튼이 없다', async () => {
    // 보는 사람과 프로필 주인이 다른 상황을 만든다.
    server.use(
      http.get('/api/v1/users/me/summary', () =>
        HttpResponse.json({
          status: 'success',
          data: { userId: 99, handle: 'someone', displayName: '남', avatarUrl: null },
        }),
      ),
    );

    renderRoute('/users/woojin');

    await screen.findByRole('heading', { name: '정우진' });

    expect(screen.queryByRole('button', { name: /프로필 사진|사진 변경/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: '구성원 인증' })).not.toBeInTheDocument();
  });

  it('저장에 실패하면 알린다', async () => {
    const user = userEvent.setup();
    server.use(http.put('/api/v1/users/me', () => new HttpResponse(null, { status: 500 })));

    renderRoute('/users/woojin');
    await screen.findByRole('button', { name: '프로필 사진 추가' });

    await pickFile(user, '프로필 사진 추가');

    expect(await screen.findByRole('alert')).toHaveTextContent(
      '프로필 사진을 저장하지 못했습니다.',
    );
  });
});
