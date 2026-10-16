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

test('관리자가 대기 중인 우아한테크코스 소속 인증 신청을 승인한다', async () => {
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

test('관리자 프로젝트 목록의 다음 페이지를 커서로 조회한다', async () => {
  const user = userEvent.setup();
  signInAs('ADMIN');
  const requestedCursors: (string | null)[] = [];
  server.use(
    http.get('/api/v1/admin/projects', ({ request }) => {
      const cursor = new URL(request.url).searchParams.get('cursor');
      requestedCursors.push(cursor);
      return HttpResponse.json({
        status: 'success',
        data: [
          {
            id: cursor ? 302 : 300,
            slug: cursor ? 'next' : 'loop',
            title: cursor ? '다음 프로젝트' : '루프',
            tagline: '프로젝트 소개',
            cohort: 8,
            members: [],
            approvalStatus: 'PENDING',
            rejectReason: null,
          },
        ],
        meta: { nextCursor: cursor ? null : 'next-page', hasNext: !cursor, totalCount: 2 },
      });
    }),
  );

  renderRoute('/admin?tab=projects');
  expect(await screen.findByRole('button', { name: '루프 상세 보기' })).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: '더 보기' }));
  expect(
    await screen.findByRole('button', { name: '다음 프로젝트 상세 보기' }),
  ).toBeInTheDocument();
  expect(requestedCursors).toEqual([null, 'next-page']);
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

test('프로젝트 배너는 slug 경로로 보이고, 수정할 때 targetSlug로 보낸다', async () => {
  const user = userEvent.setup();
  signInAs('ADMIN');
  let updateBody: unknown;
  server.use(
    http.get('/api/v1/admin/home/banners', () =>
      HttpResponse.json({
        status: 'success',
        data: [
          {
            bannerId: 2,
            mediaId: 11,
            imageUrl: 'https://placehold.co/1200x400',
            destinationType: 'TARGET',
            targetType: 'PROJECT',
            targetId: null,
            targetSlug: 'loop',
            linkType: null,
            linkUrl: null,
            displayOrder: 0,
            active: true,
            createdBy: 7,
            createdAt: '2026-09-16T00:00:00Z',
            updatedAt: '2026-09-16T00:00:00Z',
          },
        ],
      }),
    ),
    http.put('/api/v1/admin/home/banners/:bannerId', async ({ request }) => {
      updateBody = await request.json();
      return HttpResponse.json({ status: 'success', data: updateBody });
    }),
  );

  renderRoute('/admin?tab=banners');
  const list = await screen.findByRole('list');
  expect(within(list).getByText('/projects/@loop')).toBeInTheDocument();
  await user.click(within(list).getByRole('button', { name: '숨기기' }));

  expect(updateBody).toEqual(
    expect.objectContaining({ targetType: 'PROJECT', targetId: null, targetSlug: 'loop' }),
  );
});

test('관리자가 다른 사람의 프로젝트를 고치면 바꾼 칸만 보낸다', async () => {
  const user = userEvent.setup();
  signInAs('ADMIN');
  let patchBody: unknown;
  server.use(
    http.patch('/api/v1/admin/projects/:projectId/migration', async ({ params, request }) => {
      patchBody = await request.json();
      return HttpResponse.json({
        status: 'success',
        data: {
          projectId: Number(params.projectId),
          updatedFields: Object.keys(patchBody as object),
          updatedAt: '2026-09-30T00:00:00Z',
        },
      });
    }),
  );

  renderRoute('/admin?tab=project-edit');
  await user.click(await screen.findByRole('button', { name: 'shout-outz 수정' }));

  const title = await screen.findByLabelText('프로젝트 이름 *');
  await user.clear(title);
  await user.type(title, '샤웃아웃즈');
  const slug = screen.getByLabelText('slug *');
  await user.clear(slug);
  await user.type(slug, 'shoutouts');

  expect(screen.getByText('바뀐 항목: 프로젝트 이름, slug')).toBeInTheDocument();
  expect(screen.getByText(/기존 주소 \/projects\/@shout-outz/)).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: '저장' }));

  expect(await screen.findByRole('status')).toHaveTextContent('저장했어요.');
  expect(patchBody).toEqual({ title: '샤웃아웃즈', slug: 'shoutouts' });
});

describe('버그 제보', () => {
  const findReports = async () =>
    within(await screen.findByRole('list', { name: '버그 제보 목록' })).getAllByRole('listitem');

  test('관리자가 접수된 버그 제보를 보고 전체 내용을 펼친다', async () => {
    const user = userEvent.setup();
    signInAs('ADMIN');

    renderRoute('/admin?tab=bug-reports');

    const reports = await findReports();
    expect(reports).toHaveLength(1);
    expect(screen.getByText('총 1건')).toBeInTheDocument();
    expect(reports[0]).toHaveTextContent('#2');
    expect(reports[0]).toHaveTextContent('사용자 #10');
    expect(reports[0]).not.toHaveTextContent('제보한 화면');

    await user.click(within(reports[0]!).getByRole('button', { name: '전체 내용 보기' }));

    expect(
      await within(reports[0]!).findByText(/제보한 화면: \/community\/12/),
    ).toBeInTheDocument();
    expect(within(reports[0]!).getByRole('button', { name: '접기' })).toHaveAttribute(
      'aria-expanded',
      'true',
    );
  });

  test('전체 탭에서는 처리 완료된 제보와 비로그인 제보도 함께 본다', async () => {
    const user = userEvent.setup();
    signInAs('ADMIN');

    renderRoute('/admin?tab=bug-reports');
    await findReports();
    await user.click(screen.getByRole('tab', { name: '전체' }));

    await waitFor(async () => expect(await findReports()).toHaveLength(2));
    const reports = await findReports();
    expect(reports[1]).toHaveTextContent('처리 완료');
    expect(reports[1]).toHaveTextContent('비로그인 제보');
    expect(within(reports[1]!).getByRole('button', { name: '다시 접수' })).toBeInTheDocument();
  });

  test('처리 완료로 바꾸면 접수 목록에서 빠진다', async () => {
    const user = userEvent.setup();
    signInAs('ADMIN');
    let completed = false;
    let body: unknown;
    const report = {
      bugReportId: 9,
      contentPreview: '버튼이 안 눌려요.',
      reporterUserId: null,
      status: 'OPEN',
      createdAt: '2026-10-08T05:00:00Z',
      updatedAt: '2026-10-08T05:00:00Z',
      statusChangedAt: null,
      statusChangedByUserId: null,
    };
    server.use(
      http.get('/api/v1/admin/bug-reports', () =>
        HttpResponse.json({
          status: 'success',
          data: completed ? [] : [report],
          meta: { nextCursor: null, hasNext: false, totalCount: completed ? 0 : 1 },
        }),
      ),
      http.patch('/api/v1/admin/bug-reports/:bugReportId/status', async ({ request }) => {
        body = await request.json();
        completed = true;
        return HttpResponse.json({
          status: 'success',
          data: { bugReportId: 9, status: 'COMPLETED', updatedAt: '2026-10-08T06:00:00Z' },
        });
      }),
    );

    renderRoute('/admin?tab=bug-reports');
    const [row] = await findReports();
    await user.click(within(row!).getByRole('button', { name: '처리 완료' }));

    expect(await screen.findByText('접수 상태의 버그 제보가 없습니다.')).toBeInTheDocument();
    expect(body).toEqual({ status: 'COMPLETED' });
  });
});
