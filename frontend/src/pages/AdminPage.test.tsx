/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { renderRoute, server } from '@/test/renderRoute';

const signInAs = (role: 'USER' | 'ADMIN') =>
  server.use(
    http.get('/api/v1/auth/session', () =>
      HttpResponse.json({
        status: 'success',
        data: { status: 'AUTHENTICATED', userId: 1, role, csrfToken: 'token' },
      }),
    ),
  );

test('관리자가 아닌 계정은 관리 메뉴를 보지 못한다', async () => {
  signInAs('USER');

  renderRoute('/admin');

  expect(await screen.findByText('접근 권한이 없어요.')).toBeInTheDocument();
  expect(screen.queryByRole('tablist', { name: '관리 메뉴' })).not.toBeInTheDocument();
});

test('관리자가 대기 중인 크루 인증 신청을 승인한다', async () => {
  const user = userEvent.setup();
  signInAs('ADMIN');
  let approvedId: string | undefined;
  server.use(
    http.post('/api/v1/admin/verification-requests/:requestId/approve', ({ params }) => {
      approvedId = String(params.requestId);
      return HttpResponse.json({
        status: 'success',
        data: {
          requestId: 101,
          status: 'APPROVED',
          decidedAt: '2026-09-20T00:00:00Z',
          decidedBy: { userId: 7, handle: 'admin' },
        },
      });
    }),
  );

  renderRoute('/admin');
  await user.click(await screen.findByRole('button', { name: '샤를 승인' }));

  expect(approvedId).toBe('101');
});

test('반려 사유를 적어야 인증 신청을 반려할 수 있다', async () => {
  const user = userEvent.setup();
  signInAs('ADMIN');
  let rejectBody: unknown;
  server.use(
    http.post('/api/v1/admin/verification-requests/:requestId/reject', async ({ request }) => {
      rejectBody = await request.json();
      return HttpResponse.json({
        status: 'success',
        data: {
          requestId: 101,
          status: 'REJECTED',
          reason: '닉네임 확인 불가',
          decidedAt: '2026-09-20T00:00:00Z',
          decidedBy: { userId: 7, handle: 'admin' },
        },
      });
    }),
  );

  renderRoute('/admin?tab=crews');
  await user.click(await screen.findByRole('button', { name: '샤를 반려' }));
  const confirm = screen.getByRole('button', { name: '반려 확정' });
  expect(confirm).toBeDisabled();

  await user.type(screen.getByRole('textbox', { name: '샤를 반려 사유' }), '닉네임 확인 불가');
  await user.click(confirm);

  expect(rejectBody).toEqual({ reason: '닉네임 확인 불가' });
});

test('관리자 프로젝트 목록을 표시하고 ID 기반 승인 API를 호출한다', async () => {
  const user = userEvent.setup();
  signInAs('ADMIN');
  let approvedId: string | undefined;
  server.use(
    http.post('/api/v1/admin/projects/:projectId/approve', ({ params }) => {
      approvedId = String(params.projectId);
      return HttpResponse.json({
        status: 'success',
        data: {
          projectId: Number(params.projectId),
          approvalStatus: 'APPROVED',
          decidedAt: '2026-09-20T00:00:00Z',
          decidedBy: { userId: 7, handle: 'admin' },
        },
      });
    }),
  );

  renderRoute('/admin?tab=projects');
  await user.click(await screen.findByRole('button', { name: '루프 (Loop) 상세 보기' }));
  expect(await screen.findByText(/팀 회고와 액션 아이템을 공유합니다/)).toBeInTheDocument();
  await user.click(await screen.findByRole('button', { name: '루프 (Loop) 승인' }));

  await waitFor(() => expect(approvedId).toBe('300'));
});

test('관리자가 공지를 등록한다', async () => {
  const user = userEvent.setup();
  signInAs('ADMIN');
  let noticeBody: unknown;
  server.use(
    http.post('/api/v1/news/notices', async ({ request }) => {
      noticeBody = await request.json();
      return HttpResponse.json(
        {
          status: 'success',
          data: {
            id: 900,
            type: 'NOTICE',
            title: '데모데이 안내',
            summary: '일정 안내',
            body: '본문',
            author: { userId: 7, name: '샤라웃 운영팀' },
            isPinned: false,
            publishedAt: '2026-09-20T00:00:00Z',
          },
        },
        { status: 201 },
      );
    }),
  );

  renderRoute('/admin?tab=news');
  await user.type(await screen.findByRole('textbox', { name: '제목 *' }), '데모데이 안내');
  await user.type(screen.getByRole('textbox', { name: '요약 *' }), '일정 안내');
  await user.type(screen.getByRole('textbox', { name: '본문 *' }), '본문');
  await user.click(screen.getByRole('button', { name: '공지 등록' }));

  expect(noticeBody).toEqual({
    title: '데모데이 안내',
    summary: '일정 안내',
    body: '본문',
    authorName: '샤라웃 운영팀',
  });
  expect(await screen.findByRole('status')).toHaveTextContent('소식을 등록했어요.');
});

test('관리자가 등록된 홈 배너를 보고 숨길 수 있다', async () => {
  const user = userEvent.setup();
  signInAs('ADMIN');
  let updateBody: unknown;
  server.use(
    http.put('/api/v1/admin/home/banners/:bannerId', async ({ request }) => {
      updateBody = await request.json();
      return HttpResponse.json({ status: 'success', data: updateBody });
    }),
  );

  renderRoute('/admin?tab=banners');
  const list = await screen.findByRole('list');
  expect(within(list).getByText('/projects/@dropit')).toBeInTheDocument();
  await user.click(within(list).getByRole('button', { name: '숨기기' }));

  expect(updateBody).toEqual(expect.objectContaining({ mediaId: 10, active: false }));
});
