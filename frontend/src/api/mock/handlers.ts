import { http, HttpResponse } from 'msw';

import { getFeedList } from '@/api/mock/feed';
import { getNewsDetail, getNewsList } from '@/api/mock/news';
import projects from '@/api/mock/projects.json';
import { isNewsFilter } from '@/types/news';

export const handlers = [
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
