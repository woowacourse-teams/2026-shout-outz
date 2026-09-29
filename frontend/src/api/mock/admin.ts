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

const decided = (requestId: number, status: string) => ({
  requestId,
  status,
  decidedAt: '2026-09-20T00:00:00Z',
  decidedBy: { userId: 7, handle: 'admin' },
});

export const adminHandlers = [
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
    const items = status === 'PENDING' ? pendingProjects : [];
    return HttpResponse.json({
      status: 'success',
      data: items,
      meta: { nextCursor: null, hasNext: false, totalCount: items.length },
    });
  }),

  http.get('/api/v1/admin/projects/:projectId', ({ params }) => {
    const project = pendingProjects.find((item) => item.id === Number(params.projectId));
    if (!project) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json({
      status: 'success',
      data: {
        ...project,
        descriptionMd: '## 프로젝트 소개\n팀 회고와 액션 아이템을 공유합니다.',
        githubRepositoryUrl: 'https://github.com/woowacourse-teams/loop',
        deploymentUrl: null,
        viewCount: 0,
        descriptionMedia: [],
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
