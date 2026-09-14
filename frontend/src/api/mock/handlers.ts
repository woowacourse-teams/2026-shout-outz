import { http, HttpResponse } from 'msw';

import { getNewsDetail, getNewsList } from '@/api/mock/news';
import projects from '@/api/mock/projects.json';
import { isNewsFilter } from '@/types/news';

export const handlers = [
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
    const type = new URL(request.url).searchParams.get('type');
    const news = getNewsList();

    return HttpResponse.json({
      status: 'success',
      data: isNewsFilter(type) && type !== 'ALL' ? news.filter((item) => item.type === type) : news,
      meta: { nextCursor: null },
    });
  }),

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
