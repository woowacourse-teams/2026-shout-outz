import { http, HttpResponse } from 'msw';

import { getFeedList } from '@/api/mock/feed';
import { getNewsDetail, getNewsList } from '@/api/mock/news';
import { getCohorts, getTechTags, searchCrewList } from '@/api/mock/project';
import projects from '@/api/mock/projects.json';
import { isNewsFilter } from '@/types/news';

/** 미디어 업로드 시작이 내려주는 presigned PUT URL의 목 주소 */
export const MOCK_STORAGE_ORIGIN = 'https://storage.test';

export const handlers = [
  http.get('/api/v1/cohorts', () =>
    HttpResponse.json({ status: 'success', data: { items: getCohorts() } }),
  ),

  http.get('/api/v1/tech-tags', ({ request }) => {
    const keyword = new URL(request.url).searchParams.get('keyword');

    return HttpResponse.json({ status: 'success', data: { items: getTechTags(keyword) } });
  }),

  http.get('/api/v1/users/search', ({ request }) => {
    const keyword = new URL(request.url).searchParams.get('keyword') ?? '';

    return HttpResponse.json({
      status: 'success',
      data: { items: searchCrewList(keyword) },
      meta: { nextCursor: null, hasNext: false },
    });
  }),

  http.post('/api/v1/projects', () =>
    HttpResponse.json(
      {
        status: 'success',
        data: { id: 101, approvalStatus: 'PENDING', createdAt: '2026-09-16T10:00:00+09:00' },
      },
      { status: 201 },
    ),
  ),

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

  http.get('/api/v1/projects/:projectId', ({ params }) => {
    const index = projects.findIndex((_, index) => String(index + 1) === params.projectId);
    const project = projects[index];
    if (!project) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json({
      status: 'success',
      data: {
        id: index + 1,
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
        rejectReason: null,
        viewCount: 0,
        starCount: 0,
        likeCount: project.likeCount,
        bookmarkCount: project.bookmarkCount,
        likedByMe: false,
        bookmarkedByMe: false,
        commentCount: 0,
        techTags: project.techTags.map((displayName, index) => ({ id: index + 1, displayName })),
        members: project.members,
        createdAt: '2026-08-09T11:30:00+09:00',
        updatedAt: '2026-08-09T11:30:00+09:00',
      },
    });
  }),
  http.get('/api/v1/projects', () =>
    HttpResponse.json({
      status: 'success',
      // 상세 페이지용 JSON을 목록 API 응답 형식으로 변환한다.
      data: projects.map(({ name, tagline }, index) => ({
        id: index + 1,
        title: name,
        tagline,
        thumbnailUrl: '',
        cohort: 6,
        deletedAt: null,
        restoreDeadlineAt: null,
      })),
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

    return HttpResponse.json({
      status: 'success',
      data: getFeedList(sort, size),
      meta: { nextCursor: null, hasNext: false },
    });
  }),

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
];
