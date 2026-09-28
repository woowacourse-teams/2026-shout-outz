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
    destinationType: 'TARGET',
    targetType: 'PROJECT',
    targetId: 20,
    linkType: null,
    linkUrl: null,
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
    return HttpResponse.json({
      status: 'success',
      data: {
        items: verificationRequests.filter((item) => item.status === status),
        nextCursor: null,
      },
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

  // TODO 명세에 없는 관리자 프로젝트 심사 API. `src/apis/admin.ts`의 가정과 같은 모양이다.
  http.get('/api/v1/admin/projects', ({ request }) => {
    const status = new URL(request.url).searchParams.get('status') ?? 'PENDING';
    return HttpResponse.json({
      status: 'success',
      data: {
        items: status === 'PENDING' ? pendingProjects : [],
        nextCursor: null,
      },
    });
  }),

  http.post(
    '/api/v1/admin/projects/:projectId/approve',
    () => new HttpResponse(null, { status: 204 }),
  ),

  http.post(
    '/api/v1/admin/projects/:projectId/reject',
    () => new HttpResponse(null, { status: 204 }),
  ),

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
