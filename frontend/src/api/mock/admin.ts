import { http, HttpResponse } from 'msw';

const verificationRequests = [
  {
    requestId: 101,
    applicant: { userId: 42, handle: 'charles' },
    userType: 'WOOWACOURSE_CREW',
    nickname: '샤를',
    cohort: 8,
    track: 'BACKEND',
    status: 'PENDING',
    requestedAt: '2026-09-16T02:30:00Z',
  },
  {
    requestId: 102,
    applicant: { userId: 43, handle: 'coach-brown' },
    userType: 'WOOWACOURSE_COACH',
    nickname: '브라운',
    cohort: null,
    track: null,
    status: 'PENDING',
    requestedAt: '2026-09-15T08:00:00Z',
  },
];

const banners = [
  {
    bannerId: 1,
    mediaId: 10,
    imageUrl: 'https://placehold.co/1200x400',
    destinationType: 'URL',
    targetType: null,
    targetId: null,
    targetSlug: null,
    linkType: 'INTERNAL_PATH',
    linkUrl: '/projects/@dropit',
    displayOrder: 0,
    active: true,
    createdBy: 7,
    createdAt: '2026-09-16T00:00:00Z',
    updatedAt: '2026-09-16T00:00:00Z',
  },
];

const pendingProjects = [
  { id: 300, slug: 'loop', title: '루프 (Loop)', tagline: '스프린트 회고와 액션 아이템을 하나로' },
  { id: 301, slug: 'dropit', title: 'Dropit', tagline: '작은 서비스를 모아보는 아카이브' },
].map((project) => ({
  ...project,
  cohort: 8,
  thumbnailImageId: null,
  thumbnailUrl: null,
  starCount: 0,
  likeCount: 0,
  commentCount: 0,
  techTags: [],
  members: [
    { handle: 'charles', displayName: '샤를', cohort: 8, track: 'BACKEND', avatarUrl: null },
  ],
  approvalStatus: 'PENDING',
  rejectReason: null,
}));

const approvedProjects = [
  {
    ...pendingProjects[0],
    id: 200,
    slug: 'shout-outz',
    title: 'shout-outz',
    tagline: '우테코 크루들의 프로젝트와 소식을 모아보는 곳',
    techTags: [{ id: 1, displayName: 'React' }],
    approvalStatus: 'APPROVED',
  },
];

const adminProjects = [...pendingProjects, ...approvedProjects];

const decided = (requestId: number, status: string) => ({
  requestId,
  status,
  decidedAt: '2026-09-20T00:00:00Z',
  decidedBy: { userId: 7, handle: 'admin' },
});

// 상태 변경 mock(PATCH)이 값을 바꾸므로 let처럼 다룬다.
const bugReports = [
  {
    bugReportId: 2,
    contentPreview: '좋아요를 눌러도 숫자가 바로 바뀌지 않아요.',
    content: '좋아요를 눌러도 숫자가 바로 바뀌지 않아요.\n\n---\n제보한 화면: /community/12',
    reporterUserId: 10,
    status: 'OPEN' as 'OPEN' | 'COMPLETED',
    createdAt: '2026-10-08T05:00:00Z',
    updatedAt: '2026-10-08T05:00:00Z',
    statusChangedAt: null as string | null,
    statusChangedByUserId: null as number | null,
  },
  {
    bugReportId: 1,
    contentPreview: '모바일에서 프로필 사진이 깨져 보여요.',
    content: '모바일에서 프로필 사진이 깨져 보여요.\n\n---\n제보한 화면: /users/@woojin',
    reporterUserId: null as number | null,
    status: 'COMPLETED' as 'OPEN' | 'COMPLETED',
    createdAt: '2026-10-07T09:00:00Z',
    updatedAt: '2026-10-07T12:00:00Z',
    statusChangedAt: '2026-10-07T12:00:00Z' as string | null,
    statusChangedByUserId: 7 as number | null,
  },
];

// 목록은 전체 내용 대신 미리보기만 준다.
const toBugReportItem = (report: (typeof bugReports)[number]) => {
  const item: Partial<typeof report> = { ...report };
  delete item.content;
  return item;
};

export const adminHandlers = [
  http.get('/api/v1/admin/bug-reports', ({ request }) => {
    const status = new URL(request.url).searchParams.get('status') ?? 'OPEN';
    const items = bugReports.filter((item) => status === 'ALL' || item.status === status);

    return HttpResponse.json({
      status: 'success',
      data: items.map(toBugReportItem),
      meta: { nextCursor: null, hasNext: false, totalCount: items.length },
    });
  }),
  http.get('/api/v1/admin/bug-reports/:bugReportId', ({ params }) => {
    const report = bugReports.find((item) => item.bugReportId === Number(params.bugReportId));
    if (!report) {
      return HttpResponse.json(
        { status: 'error', code: 'BUG_REPORT_NOT_FOUND', message: '버그 제보를 찾을 수 없습니다.' },
        { status: 404 },
      );
    }
    // 상세는 미리보기 대신 전체 내용을 준다.
    const detail: Partial<typeof report> = { ...report };
    delete detail.contentPreview;
    return HttpResponse.json({ status: 'success', data: detail });
  }),
  http.patch('/api/v1/admin/bug-reports/:bugReportId/status', async ({ params, request }) => {
    const { status } = (await request.json()) as { status: 'OPEN' | 'COMPLETED' };
    const report = bugReports.find((item) => item.bugReportId === Number(params.bugReportId));
    if (!report) return new HttpResponse(null, { status: 404 });
    const now = new Date().toISOString();
    Object.assign(report, {
      status,
      statusChangedAt: now,
      statusChangedByUserId: 7,
      updatedAt: now,
    });
    const { bugReportId, statusChangedAt, statusChangedByUserId, updatedAt } = report;
    return HttpResponse.json({
      status: 'success',
      data: { bugReportId, status, statusChangedAt, statusChangedByUserId, updatedAt },
    });
  }),
  http.get('/api/v1/admin/verification-requests', ({ request }) => {
    const status = new URL(request.url).searchParams.get('status') ?? 'PENDING';
    const items = verificationRequests.filter((item) => item.status === status);

    // 목록은 data에 배열로, 커서는 meta에 담긴다.
    return HttpResponse.json({
      status: 'success',
      data: items,
      meta: { nextCursor: null, hasNext: false, totalCount: items.length },
    });
  }),

  http.post('/api/v1/admin/verification-requests/:requestId/approve', ({ params }) =>
    HttpResponse.json({ status: 'success', data: decided(Number(params.requestId), 'APPROVED') }),
  ),

  http.post(
    '/api/v1/admin/verification-requests/:requestId/reject',
    async ({ params, request }) => {
      const { reason } = (await request.json()) as { reason: string };
      return HttpResponse.json({
        status: 'success',
        data: { ...decided(Number(params.requestId), 'REJECTED'), reason },
      });
    },
  ),

  http.get('/api/v1/admin/projects', ({ request }) => {
    const status = new URL(request.url).searchParams.get('status') ?? 'PENDING';
    const items = adminProjects.filter((item) => item.approvalStatus === status);
    return HttpResponse.json({
      status: 'success',
      data: items,
      meta: { nextCursor: null, hasNext: false, totalCount: items.length },
    });
  }),

  http.get('/api/v1/admin/projects/:projectId', ({ params }) => {
    const project = adminProjects.find((item) => item.id === Number(params.projectId));
    if (!project) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json({
      status: 'success',
      data: {
        ...project,
        teamName: '팀 루프',
        serviceStatus: 'CLOSED',
        imageUrl: null,
        descriptionMd: '## 프로젝트 소개\n팀 회고와 액션 아이템을 공유합니다.',
        githubRepositoryUrl: 'https://github.com/woowacourse-teams/loop',
        deploymentUrl: null,
        viewCount: 0,
        descriptionMedia: [],
      },
    });
  }),

  http.patch('/api/v1/admin/projects/:projectId/migration', async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json({
      status: 'success',
      data: {
        projectId: Number(params.projectId),
        updatedFields: Object.keys(body),
        updatedAt: '2026-09-30T00:00:00Z',
      },
    });
  }),

  http.post('/api/v1/admin/projects/:projectId/approve', ({ params }) =>
    HttpResponse.json({
      status: 'success',
      data: {
        projectId: Number(params.projectId),
        approvalStatus: 'APPROVED',
        decidedAt: '2026-09-20T00:00:00Z',
        decidedBy: { userId: 7, handle: 'admin' },
      },
    }),
  ),

  http.post('/api/v1/admin/projects/:projectId/reject', async ({ params, request }) => {
    const { reason } = (await request.json()) as { reason: string };
    return HttpResponse.json({
      status: 'success',
      data: {
        projectId: Number(params.projectId),
        approvalStatus: 'REJECTED',
        reason,
        decidedAt: '2026-09-20T00:00:00Z',
        decidedBy: { userId: 7, handle: 'admin' },
      },
    });
  }),

  http.post('/api/v1/news/notices', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json(
      {
        status: 'success',
        data: {
          id: 900,
          type: 'NOTICE',
          ...body,
          author: { userId: 7, name: body.authorName },
          isPinned: false,
          publishedAt: '2026-09-20T00:00:00Z',
        },
      },
      { status: 201 },
    );
  }),

  http.post('/api/v1/news/events', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json(
      {
        status: 'success',
        data: {
          id: 901,
          type: 'EVENT',
          ...body,
          author: { userId: 7, name: body.authorName },
          isPinned: false,
          publishedAt: '2026-09-20T00:00:00Z',
        },
      },
      { status: 201 },
    );
  }),

  http.get('/api/v1/admin/home/banners', () =>
    HttpResponse.json({ status: 'success', data: banners }),
  ),

  http.post('/api/v1/admin/home/banners', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json(
      {
        status: 'success',
        data: {
          ...body,
          bannerId: 2,
          imageUrl: 'https://placehold.co/1200x400',
          createdBy: 7,
          createdAt: '2026-09-20T00:00:00Z',
          updatedAt: '2026-09-20T00:00:00Z',
        },
      },
      { status: 201 },
    );
  }),

  http.put('/api/v1/admin/home/banners/:bannerId', async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json({
      status: 'success',
      data: { ...banners[0], ...body, bannerId: Number(params.bannerId) },
    });
  }),

  http.delete('/api/v1/admin/home/banners/:bannerId', ({ params }) =>
    HttpResponse.json({ status: 'success', data: { id: Number(params.bannerId) } }),
  ),
];
