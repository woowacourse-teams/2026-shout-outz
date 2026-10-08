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
    expect(screen.getAllByText('우아한테크코스 8기 백엔드').length).toBeGreaterThan(0);
    expect(
      screen.getByText('대규모 트래픽 분산 처리와 데이터 정합성에 집착하는 백엔드 개발자입니다.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'GitHub' })).toHaveAttribute(
      'href',
      'https://github.com/woojin-dev',
    );
    expect(
      screen.queryByRole('link', { name: '우아한테크코스 소속 인증' }),
    ).not.toBeInTheDocument();
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
      expect(projects[1]!.closest('a')).toHaveAttribute('href', '/projects/@dropit/edit');
      expect(within(projects[1]!).getByRole('status')).toHaveTextContent('승인 대기');
      expect(projects[2]).toHaveTextContent('스터디 메이트');
      expect(projects[2]!.closest('a')).toHaveAttribute('href', '/projects/@study-mate/edit');
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
    const error = jest.spyOn(console, 'error').mockImplementation(() => {});
    try {
      renderRoute('/users/nobody');

      expect(await screen.findByText('화면을 불러오지 못했습니다.')).toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: '정우진' })).not.toBeInTheDocument();
    } finally {
      error.mockRestore();
    }
  });
});

describe('프로필 수정', () => {
  const generalProfile = {
    userId: 10,
    handle: 'woojin',
    displayName: '정우진',
    userType: 'GENERAL',
    track: null,
    cohort: null,
    bio: null,
    avatarImageId: 3,
    avatarUrl: 'https://example.com/me.png',
    githubProfileUrl: null,
    blogUrl: null,
    counts: { projects: 0, feeds: 0 },
  };

  const openEditModal = async (user: ReturnType<typeof userEvent.setup>) => {
    await user.click(await screen.findByRole('button', { name: '프로필 수정' }));
    return screen.findByRole('dialog');
  };

  const catchUpdate = () => {
    const request: { body?: unknown } = {};
    server.use(
      http.put('/api/v1/users/me', async ({ request: req }) => {
        request.body = await req.json();
        return HttpResponse.json({
          status: 'success',
          data: { handle: 'woojin', displayName: '정우진', userType: 'WOOWACOURSE_CREW' },
        });
      }),
    );
    return request;
  };

  it('인증 전 사용자는 닉네임을 고칠 수 있다', async () => {
    const user = userEvent.setup();
    server.use(
      http.get('/api/v1/users/woojin', () =>
        HttpResponse.json({ status: 'success', data: generalProfile }),
      ),
    );
    const request = catchUpdate();

    renderRoute('/users/woojin');
    const dialog = await openEditModal(user);
    const nickname = within(dialog).getByLabelText('닉네임');
    expect(nickname).toBeEnabled();
    await user.clear(nickname);
    await user.type(nickname, '우진');
    await user.click(within(dialog).getByRole('button', { name: '저장' }));

    // 프로필 수정은 전체 교체라 지금 사진, 소개, 링크도 그대로 다시 보낸다.
    await waitFor(() =>
      expect(request.body).toEqual({
        displayName: '우진',
        bio: null,
        githubProfileUrl: null,
        blogUrl: null,
        avatarImageId: 3,
      }),
    );
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('우아한테크코스 소속 인증을 마친 사용자는 닉네임은 막히고 사진은 바꿀 수 있다', async () => {
    const user = userEvent.setup();
    const request = catchUpdate();

    renderRoute('/users/woojin');
    const dialog = await openEditModal(user);
    expect(within(dialog).getByLabelText('닉네임')).toBeDisabled();
    expect(within(dialog).getByLabelText('닉네임')).toHaveAccessibleDescription(
      '우아한테크코스 소속 인증을 마친 사용자는 닉네임을 바꿀 수 없어요.',
    );

    // 버튼은 숨겨진 file input을 대신 눌러 주는 것이라, 테스트는 input에 직접 올린다.
    await user.upload(
      within(dialog).getByLabelText('프로필 사진 추가'),
      new File(['x'], 'me.png', { type: 'image/png' }),
    );
    await within(dialog).findByRole('button', { name: '사진 변경' });
    await user.click(within(dialog).getByRole('button', { name: '저장' }));

    await waitFor(() => expect(request.body).toHaveProperty('avatarImageId', 12));
    expect(request.body).toHaveProperty('displayName', '정우진');
  });

  it('기본 이미지로 바꾸면 사진 없이 저장한다', async () => {
    const user = userEvent.setup();
    server.use(
      http.get('/api/v1/users/woojin', () =>
        HttpResponse.json({ status: 'success', data: generalProfile }),
      ),
    );
    const request = catchUpdate();

    renderRoute('/users/woojin');
    const dialog = await openEditModal(user);
    await user.click(within(dialog).getByRole('button', { name: '기본 이미지로' }));
    await user.click(within(dialog).getByRole('button', { name: '저장' }));

    await waitFor(() => expect(request.body).toHaveProperty('avatarImageId', null));
  });

  it('저장에 실패하면 모달 안에서 알린다', async () => {
    const user = userEvent.setup();
    server.use(http.put('/api/v1/users/me', () => new HttpResponse(null, { status: 500 })));

    renderRoute('/users/woojin');
    const dialog = await openEditModal(user);
    await user.click(within(dialog).getByRole('button', { name: '저장' }));

    expect(await within(dialog).findByRole('alert')).toHaveTextContent(
      '요청에 실패했습니다. 다시 시도해 주세요.',
    );
  });

  it('남의 프로필에는 수정 버튼이 없다', async () => {
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

    expect(screen.queryByRole('button', { name: '프로필 수정' })).not.toBeInTheDocument();
    expect(
      screen.queryByRole('link', { name: '우아한테크코스 소속 인증' }),
    ).not.toBeInTheDocument();
  });
});
