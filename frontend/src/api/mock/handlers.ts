import { http, HttpResponse } from 'msw';

import { adminHandlers } from '@/api/mock/admin';
import { homeBanners } from '@/api/mock/home';
import { getFeedList } from '@/api/mock/feed';
import { getNewsDetail, getNewsList } from '@/api/mock/news';
import { notificationHandlers } from '@/api/mock/notification';
import { getCohorts, getTechTags, searchCrewList } from '@/api/mock/project';
import projects from '@/api/mock/projects.json';
import { getUserFeeds, getUserProfile, getUserProjects, updateProfile } from '@/api/mock/user';
import { getMockProjectReaction, setMockProjectLike } from '@/api/mock/reactions';
import { isNewsFilter } from '@/types/news';
import type { ProjectUpdateRequest } from '@/types/project';

/** 미디어 업로드 시작이 내려주는 presigned PUT URL의 목 주소 */
export const MOCK_STORAGE_ORIGIN = 'https://storage.test';

const toProjectMember = (member: (typeof projects)[number]['members'][number]) => ({
  userId: member.userId,
  handle: member.userId === 1 ? 'woojin' : `crew${member.userId}`,
  displayName: member.displayName,
  userType: 'WOOWACOURSE_CREW' as const,
  cohort: member.cohort,
  track: member.track === 'BE' ? 'BACKEND' : 'FRONTEND',
  avatarUrl: member.avatarUrl,
  githubAvatarUrl: null,
  githubProfileUrl: null,
});

const updatedProjects = new Map<string, Record<string, unknown>>();

export const handlers = [
  http.get('/api/v1/auth/session', () =>
    HttpResponse.json({
      status: 'success',
      data: {
        status: 'AUTHENTICATED',
        userId: 1,
        role: 'USER',
        csrfToken: 'development-token',
      },
    }),
  ),

  http.post('/api/v1/auth/signup', () =>
    HttpResponse.json({ status: 'success', data: { userId: 1 } }, { status: 201 }),
  ),

  http.post('/api/v1/auth/logout', () => new HttpResponse(null, { status: 204 })),

  http.get('/api/v1/users/me/verification-request', () =>
    HttpResponse.json({
      status: 'success',
      data: {
        requestId: 1,
        userType: 'WOOWACOURSE_CREW',
        nickname: '우진',
        cohort: 8,
        track: 'BACKEND',
        status: 'APPROVED',
        requestedAt: '2026-09-16T00:00:00Z',
        decidedAt: '2026-09-16T01:00:00Z',
        reason: null,
      },
    }),
  ),

  http.post('/api/v1/users/me/verification-requests', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json(
      {
        status: 'success',
        data: {
          requestId: 2,
          ...body,
          status: 'PENDING',
          requestedAt: '2026-09-19T00:00:00Z',
          decidedAt: null,
          reason: null,
        },
      },
      { status: 201 },
    );
  }),

  http.get('/api/v1/cohorts', () =>
    HttpResponse.json({ status: 'success', data: { items: getCohorts() } }),
  ),

  http.get('/api/v1/tech-tags', ({ request }) => {
    const keyword = new URL(request.url).searchParams.get('keyword');

    return HttpResponse.json({ status: 'success', data: { items: getTechTags(keyword) } });
  }),

  http.get('/api/v1/users/search', ({ request }) => {
    const keyword = new URL(request.url).searchParams.get('keyword') ?? '';

    const items = searchCrewList(keyword);

    // 실서버는 data를 배열로 직접 준다. items 래퍼를 쓰면 mock만 통과하고 실서버에서 깨진다.
    return HttpResponse.json({
      status: 'success',
      data: items,
      meta: { nextCursor: null, hasNext: false, totalCount: items.length },
    });
  }),

  http.post('/api/v1/projects', () =>
    HttpResponse.json(
      {
        status: 'success',
        data: { slug: 'new-project' },
      },
      { status: 201 },
    ),
  ),

  http.put('/api/v1/projects/@:slug', async ({ params, request }) => {
    const slug = String(params.slug);
    const index = projects.findIndex((project) => project.id === slug);
    if (index < 0) return new HttpResponse(null, { status: 404 });

    const update = (await request.json()) as ProjectUpdateRequest;
    const knownMembers = projects.flatMap((item) => item.members.map(toProjectMember));
    const members = update.memberHandles
      .map((handle) => {
        const known = knownMembers.find((member) => member.handle === handle);
        if (known) return known;
        const crew = searchCrewList(handle).find((item) => item.handle === handle);
        return crew ? { ...crew, githubAvatarUrl: null, githubProfileUrl: null } : undefined;
      })
      .filter((member) => member !== undefined);

    updatedProjects.set(slug, {
      title: update.title,
      teamName: update.teamName,
      tagline: update.tagline,
      cohort: update.cohort,
      thumbnailImageId: update.thumbnailImageId ?? null,
      githubRepositoryUrl: update.githubRepositoryUrl,
      deploymentUrl: update.deploymentUrl,
      descriptionMd: update.descriptionMd,
      serviceStatus: update.serviceStatus,
      techTags: update.techTagIds.flatMap((id) => {
        const tag = getTechTags().find((item) => item.id === id);
        return tag ? [tag] : [];
      }),
      members,
    });

    return HttpResponse.json({
      status: 'success',
      data: { slug, approvalStatus: 'APPROVED' },
    });
  }),

  // 미디어 API는 다른 API와 달리 {status, data} 봉투 없이 그대로 내려준다.
  http.post('/api/v1/media/uploads', () =>
    HttpResponse.json(
      {
        mediaId: 12,
        status: 'PENDING_UPLOAD',
        uploadUrl: `${MOCK_STORAGE_ORIGIN}/media/12`,
        expiresAt: '2026-09-16T10:05:00+09:00',
        contentType: 'image/png',
      },
      { status: 201 },
    ),
  ),

  http.put(`${MOCK_STORAGE_ORIGIN}/media/:mediaId`, () => new HttpResponse(null, { status: 200 })),

  http.post('/api/v1/media/:mediaId/complete', ({ params }) =>
    HttpResponse.json({
      mediaId: Number(params.mediaId),
      status: 'PROCESSING',
      sizeBytes: 1024,
      contentType: 'image/png',
      uploadedAt: '2026-09-16T10:01:00+09:00',
    }),
  ),

  // complete가 PROCESSING으로 응답하므로, 쓸 수 있는지는 여기서 확인한다.
  // 개발 환경에서는 기다릴 이유가 없어 바로 READY로 준다.
  http.get('/api/v1/media/:mediaId/status', ({ params }) =>
    HttpResponse.json({
      status: 'success',
      data: { mediaId: Number(params.mediaId), status: 'READY' },
    }),
  ),

  http.get('/api/v1/projects/filters', ({ request }) => {
    const selected = new URL(request.url).searchParams.get('techTagIds')?.split(',') ?? [];

    return HttpResponse.json({
      status: 'success',
      data: {
        // 고른 조건이 늘수록 남는 수가 줄어드는 것만 흉내 낸다.
        matchedProjectCount: Math.max(projects.length - selected.length, 0),
        cohorts: getCohorts().map(({ cohort, year }) => ({ cohort, year, projectCount: cohort })),
        techTags: getTechTags().map((tag) => ({ ...tag, projectCount: tag.id * 2 })),
      },
    });
  }),
  // 실서버는 상세를 slug로만 받는다. 숫자 id로는 405다.
  http.get('/api/v1/projects/@:slug', ({ params }) => {
    const index = projects.findIndex((project) => project.id === params.slug);
    const project = projects[index];
    if (!project) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json({
      status: 'success',
      data: {
        slug: project.id,
        title: project.name,
        teamName: project.teamName,
        tagline: project.tagline,
        cohort: project.cohort,
        thumbnailUrl: null,
        descriptionMd: project.descriptionMd ?? project.description,
        githubRepositoryUrl: project.githubRepositoryUrl,
        deploymentUrl: project.deploymentUrl,
        serviceStatus: project.serviceStatus,
        approvalStatus: 'APPROVED',
        editable: index === 0,
        rejectReason: null,
        viewCount: 0,
        starCount: 0,
        ...getMockProjectReaction(project.id, project.likeCount),
        bookmarkCount: project.bookmarkCount,
        bookmarkedByMe: false,
        commentCount: 0,
        techTags: project.techTags.map((displayName, index) => ({ id: index + 1, displayName })),
        members: project.members.map(toProjectMember),
        createdAt: '2026-08-09T11:30:00+09:00',
        updatedAt: '2026-08-09T11:30:00+09:00',
        ...updatedProjects.get(project.id),
      },
    });
  }),
  http.put('/api/v1/projects/@:slug/reactions/LIKE', ({ params }) => {
    const slug = String(params.slug);
    const project = projects.find((item) => item.id === slug);
    if (!project) return new HttpResponse(null, { status: 404 });
    const reaction = setMockProjectLike(slug, project.likeCount, true);
    return HttpResponse.json({
      status: 'success',
      data: {
        slug,
        type: 'LIKE',
        active: true,
        ...reaction,
        bookmarkCount: project.bookmarkCount,
      },
    });
  }),
  http.delete('/api/v1/projects/@:slug/reactions/LIKE', ({ params }) => {
    const slug = String(params.slug);
    const project = projects.find((item) => item.id === slug);
    if (!project) return new HttpResponse(null, { status: 404 });
    const reaction = setMockProjectLike(slug, project.likeCount, false);
    return HttpResponse.json({
      status: 'success',
      data: {
        slug,
        type: 'LIKE',
        active: false,
        ...reaction,
        bookmarkCount: project.bookmarkCount,
      },
    });
  }),
  http.get('/api/v1/projects', () =>
    HttpResponse.json({
      status: 'success',
      // 상세 페이지용 JSON을 목록 API 응답 형식으로 변환한다.
      data: projects.map(({ id, name, tagline, cohort, likeCount, techTags, members }) => ({
        slug: id,
        title: name,
        tagline,
        cohort,
        thumbnailUrl: null,
        ...getMockProjectReaction(id, likeCount),
        commentCount: 0,
        techTags: techTags.map((displayName, tagIndex) => ({ id: tagIndex + 1, displayName })),
        members: members.map(toProjectMember),
        ...updatedProjects.get(id),
      })),
      meta: { nextCursor: null, hasNext: false, totalCount: projects.length },
    }),
  ),

  http.get('/api/v1/news', ({ request }) => {
    const searchParams = new URL(request.url).searchParams;
    const type = searchParams.get('type');
    const eventStatus = searchParams.get('eventStatus') === 'ONGOING' ? 'ONGOING' : undefined;
    const size = Number(searchParams.get('size') ?? 20);
    const news = getNewsList(eventStatus);
    const filtered =
      isNewsFilter(type) && type !== 'ALL' ? news.filter((item) => item.type === type) : news;

    return HttpResponse.json({
      status: 'success',
      data: filtered.slice(0, size),
      meta: { nextCursor: null },
    });
  }),

  http.get('/api/v1/feeds', ({ request }) => {
    const searchParams = new URL(request.url).searchParams;
    const sort = searchParams.get('sort') === 'POPULAR' ? 'POPULAR' : 'LATEST';
    const size = Number(searchParams.get('size') ?? 20);
    const type = searchParams.get('type');
    const feedType = type === 'QUESTION' || type === 'POST' ? type : undefined;

    return HttpResponse.json({
      status: 'success',
      data: getFeedList(sort, size, feedType),
      meta: { nextCursor: null, hasNext: false },
    });
  }),

  http.get('/api/v1/users/me/summary', () =>
    HttpResponse.json({
      status: 'success',
      data: { userId: 1, handle: 'woojin', displayName: '정우진', avatarUrl: null },
    }),
  ),

  http.get('/api/v1/users/me', () =>
    HttpResponse.json({ status: 'success', data: getUserProfile('woojin') }),
  ),
  http.put('/api/v1/users/me', async ({ request }) => {
    const body = (await request.json()) as {
      displayName: string;
      bio?: string | null;
      blogUrl?: string | null;
      githubProfileUrl?: string | null;
      avatarImageId?: number | null;
    };

    if (!body.displayName?.trim()) {
      return HttpResponse.json(
        { status: 'error', code: 'VALIDATION_ERROR', message: '표시 이름을 확인해 주세요.' },
        { status: 400 },
      );
    }

    // 서버는 mediaId를 공개 URL로 바꿔서 돌려준다. null이면 기본 프로필로 돌아간다.
    const data = updateProfile({
      displayName: body.displayName,
      bio: body.bio ?? null,
      blogUrl: body.blogUrl ?? null,
      githubProfileUrl: body.githubProfileUrl ?? null,
      avatarUrl:
        body.avatarImageId == null ? null : `${MOCK_STORAGE_ORIGIN}/media/${body.avatarImageId}`,
    });

    return HttpResponse.json({ status: 'success', data });
  }),

  http.get('/api/v1/users/:handle', ({ params }) => {
    const profile = getUserProfile(String(params.handle));

    if (!profile) {
      return HttpResponse.json(
        { status: 'error', code: 'RESOURCE_NOT_FOUND', message: '요청한 리소스를 찾을 수 없음' },
        { status: 404 },
      );
    }

    return HttpResponse.json({ status: 'success', data: profile });
  }),

  http.get('/api/v1/users/:handle/projects', ({ params }) =>
    HttpResponse.json({
      status: 'success',
      data: getUserProjects(String(params.handle)),
      meta: { nextCursor: null, hasNext: false },
    }),
  ),

  http.get('/api/v1/users/:handle/feeds', ({ params }) =>
    HttpResponse.json({
      status: 'success',
      data: getUserFeeds(String(params.handle)),
      meta: { nextCursor: null, hasNext: false },
    }),
  ),

  http.get('/api/v1/home/banners', () =>
    HttpResponse.json({
      status: 'success',
      data: homeBanners,
    }),
  ),

  http.get('/api/v1/home/statistics', () =>
    HttpResponse.json({
      status: 'success',
      data: { projectCount: 128, feedCount: 341, currentCohort: 8, ongoingEventCount: 2 },
    }),
  ),

  http.get('/api/v1/news/:newsId', ({ params }) => {
    const news = getNewsDetail(Number(params.newsId));

    if (!news) {
      return HttpResponse.json(
        { status: 'error', code: 'RESOURCE_NOT_FOUND', message: '요청한 리소스를 찾을 수 없음' },
        { status: 404 },
      );
    }

    return HttpResponse.json({ status: 'success', data: news });
  }),

  ...adminHandlers,
  ...notificationHandlers,
];
