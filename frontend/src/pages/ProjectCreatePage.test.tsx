/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderRoute, server } from '@/test/renderRoute';

type User = ReturnType<typeof userEvent.setup>;

const FORM = {
  title: '루프 (Loop)',
  teamName: '루프팀',
  tagline: '스프린트 회고와 액션 아이템을 하나로 엮은 실시간 협업 도구',
  githubRepositoryUrl: 'https://github.com/woowacourse-teams/2026-loop',
  deploymentUrl: 'https://loop.team',
  descriptionMd: '## 문제\n회고 도구와 액션 아이템 관리가 흩어져 있습니다.',
};

const submit = async (user: User) => {
  await user.click(screen.getByRole('button', { name: '프로젝트 등록하기' }));
};

/** 등록 요청 본문을 가로채 돌려준다. */
const captureCreateRequest = () => {
  const received: { body?: unknown } = {};

  server.use(
    http.post('/api/v1/projects', async ({ request }) => {
      received.body = await request.json();

      return HttpResponse.json(
        {
          status: 'success',
          data: { id: 101, approvalStatus: 'PENDING', createdAt: '2026-09-16T10:00:00+09:00' },
        },
        { status: 201 },
      );
    }),
  );

  return received;
};

const fillRequiredFields = async (user: User) => {
  // 기수 목록을 불러오는 동안에는 폼 대신 로딩 문구가 떠 있다.
  await user.type(await screen.findByRole('textbox', { name: /프로젝트 이름/ }), FORM.title);
  await user.type(screen.getByRole('textbox', { name: /한 줄 소개/ }), FORM.tagline);

  await user.click(screen.getByRole('combobox', { name: /우테코 기수/ }));
  await user.click(screen.getByRole('option', { name: '6기 (2024)' }));

  await user.type(
    screen.getByRole('textbox', { name: /GitHub 레포지토리 URL/ }),
    FORM.githubRepositoryUrl,
  );

  await user.type(screen.getByRole('textbox', { name: '기술 스택 ID' }), '1');
  await user.click(screen.getByRole('button', { name: '기술 스택 추가' }));
  await screen.findByText('React');

  await user.type(screen.getByRole('searchbox', { name: '참여 팀원 검색' }), '재키');
  await user.click(screen.getByRole('button', { name: '검색' }));
  await user.click(await screen.findByRole('button', { name: /재키/ }));
};

describe('ProjectCreatePage', () => {
  it('구성원 인증을 받지 않은 사용자는 등록 폼 대신 인증 신청 안내를 본다', async () => {
    server.use(
      http.get('/api/v1/users/me/verification-request', () =>
        HttpResponse.json({ status: 'success', data: null }),
      ),
    );

    renderRoute('/projects/new');

    expect(await screen.findByText('구성원 인증이 필요해요.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '구성원 인증 신청' })).toHaveAttribute(
      'href',
      '/mypage/verification',
    );
    expect(screen.queryByRole('textbox', { name: /프로젝트 이름/ })).not.toBeInTheDocument();
  });

  it('명세의 필드를 모두 입력해 등록한다', async () => {
    const user = userEvent.setup();
    const received = captureCreateRequest();
    renderRoute('/projects/new');

    await fillRequiredFields(user);
    await user.type(screen.getByRole('textbox', { name: /팀 이름/ }), FORM.teamName);
    await user.type(screen.getByRole('textbox', { name: /서비스 배포 URL/ }), FORM.deploymentUrl);
    await user.type(screen.getByRole('textbox', { name: /상세 설명/ }), FORM.descriptionMd);
    await submit(user);

    await waitFor(() =>
      expect(received.body).toEqual({
        title: FORM.title,
        teamName: FORM.teamName,
        tagline: FORM.tagline,
        cohort: 6,
        thumbnailMediaId: null,
        githubRepositoryUrl: FORM.githubRepositoryUrl,
        deploymentUrl: FORM.deploymentUrl,
        descriptionMd: FORM.descriptionMd,
        techTagIds: [1],
        memberHandles: ['zzaekkii'],
      }),
    );
  });

  describe('등록 성공', () => {
    it('완료 안내와 프로젝트 목록으로 가는 버튼을 보여준다', async () => {
      const user = userEvent.setup();
      renderRoute('/projects/new');

      await fillRequiredFields(user);
      await submit(user);

      expect(await screen.findByText('등록이 완료됐어요.')).toBeInTheDocument();
      expect(screen.getByRole('link', { name: '프로젝트 목록으로' })).toHaveAttribute(
        'href',
        '/projects',
      );
      expect(screen.queryByRole('textbox', { name: /프로젝트 이름/ })).not.toBeInTheDocument();
    });
  });

  describe('입력 검증', () => {
    it('필수 항목이 비어 있으면 알리고 요청하지 않는다', async () => {
      const user = userEvent.setup();
      const received = captureCreateRequest();
      renderRoute('/projects/new');

      await screen.findByRole('textbox', { name: /프로젝트 이름/ });
      await submit(user);

      expect(await screen.findByText('프로젝트 이름을 입력해 주세요.')).toBeInTheDocument();
      expect(screen.getByText('한 줄 소개를 입력해 주세요.')).toBeInTheDocument();
      expect(screen.getByText('우테코 기수를 선택해 주세요.')).toBeInTheDocument();
      expect(screen.getByText('GitHub 레포지토리 URL을 입력해 주세요.')).toBeInTheDocument();
      expect(screen.getByText('기술 스택을 1개 이상 선택해 주세요.')).toBeInTheDocument();
      expect(screen.getByText('참여 팀원을 1명 이상 선택해 주세요.')).toBeInTheDocument();
      expect(received.body).toBeUndefined();
    });

    it('고치고 다시 제출하면 오류 문구가 사라지고 등록된다', async () => {
      const user = userEvent.setup();
      renderRoute('/projects/new');

      await screen.findByRole('textbox', { name: /프로젝트 이름/ });
      await submit(user);
      await screen.findByText('프로젝트 이름을 입력해 주세요.');

      await fillRequiredFields(user);
      await submit(user);

      expect(await screen.findByText('등록이 완료됐어요.')).toBeInTheDocument();
    });
  });

  describe('등록 실패', () => {
    it('서버가 거절하면 알리고 입력을 유지한다', async () => {
      server.use(http.post('/api/v1/projects', () => new HttpResponse(null, { status: 400 })));
      const user = userEvent.setup();
      renderRoute('/projects/new');

      await fillRequiredFields(user);
      await submit(user);

      expect(await screen.findByText('프로젝트 등록에 실패했습니다.')).toBeInTheDocument();
      expect(screen.getByRole('textbox', { name: /프로젝트 이름/ })).toHaveValue(FORM.title);
    });
  });
});
