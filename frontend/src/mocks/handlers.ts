import { http, HttpResponse } from 'msw';
import type { Feed } from '@/apis/feed';
import type { FeedComment } from '@/apis/feed-comment';
export const mockFeeds: Feed[] = Array.from({ length: 6 }, (_, index) => ({
  feedId: index + 1,
  content: [
    'Redis Pub/Sub으로 WebSocket 동기화 지연을 개선한 경험을 공유합니다.\n\n캐시 무효화와 메시지 순서를 함께 고민했어요.\n\nhttps://woojin.log/tech/redis-pub-sub',
    'TanStack Query를 사용하면서 배운 서버 상태 관리 이야기입니다.\n\n```ts\nawait queryClient.invalidateQueries({ queryKey: ["projects"] });\n```',
    '프로젝트에서 가장 기억에 남는 트러블슈팅은 무엇인가요?',
  ][index % 3]!,
  author: {
    handle: `crew${index}`,
    displayName: ['정우진', '김도현', '이지민'][index % 3]!,
    userType: 'WOOWACOURSE_CREW',
    track: index % 2 ? 'FRONTEND' : 'BACKEND',
    cohort: 6,
    avatarUrl: null,
  },
  categories: [{ categoryId: 1, slug: 'backend', displayName: '개발 이야기', type: 'GENERAL' }],
  media: [],
  createdAt: new Date(Date.UTC(2026, 8, 14, 9 - index)).toISOString(),
  updatedAt: new Date(Date.UTC(2026, 8, 14, 9 - index)).toISOString(),
}));
export function createFeedHandlers() {
  let sequence = 100;
  const feeds = [...mockFeeds];
  const comments = new Map<number, FeedComment[]>();
  const getComments = (id: number) => {
    if (!comments.has(id))
      comments.set(id, [
        {
          id: id * 10,
          content: '경험을 공유해 주셔서 감사합니다!',
          author: { userId: 1, displayName: '개발용 사용자', avatarUrl: null },
          parentId: null,
          createdAt: '2026-09-14T00:00:00Z',
          updatedAt: '2026-09-14T00:00:00Z',
          editable: true,
          edited: false,
          deleted: false,
        },
      ]);
    return comments.get(id)!;
  };
  return [
    http.get('/api/v1/categories', () =>
      HttpResponse.json({
        status: 'success',
        data: [
          {
            categoryId: 1,
            slug: 'backend',
            displayName: '백엔드',
            type: 'GENERAL',
            displayOrder: 1,
          },
          {
            categoryId: 3,
            slug: 'tecode-talk',
            displayName: '테코드톡',
            type: 'EVENT',
            displayOrder: 2,
          },
        ],
      }),
    ),
    http.get('/api/v1/users/me', () =>
      HttpResponse.json({
        status: 'success',
        data: {
          ...mockFeeds[0]!.author,
          bio: null,
          githubProfileUrl: null,
          blogUrl: null,
          counts: { projects: 0, feeds: 1 },
        },
      }),
    ),
    http.post('/api/v1/feeds', async ({ request }) => {
      const body = (await request.json()) as {
        content: string;
        categoryIds: number[];
        mediaIds: number[];
      };
      if (
        !body.content?.trim() ||
        Array.from(body.content).length > 500 ||
        body.categoryIds?.length !== 1 ||
        body.categoryIds[0] !== 1 ||
        body.mediaIds?.length !== 0
      ) {
        return HttpResponse.json(
          {
            status: 'error',
            code: 'VALIDATION_ERROR',
            message: '본문과 카테고리를 확인해 주세요.',
          },
          { status: 400 },
        );
      }
      const feed: Feed = {
        ...mockFeeds[0]!,
        feedId: sequence++,
        content: body.content,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      feeds.unshift(feed);
      return HttpResponse.json({ status: 'success', data: feed }, { status: 201 });
    }),
    http.get('/api/v1/users/me/summary', () =>
      HttpResponse.json({
        status: 'success',
        data: { handle: 'crew0', displayName: '정우진', avatarUrl: null },
      }),
    ),
    http.put('/api/v1/feeds/:feedId', async ({ params, request }) => {
      const index = feeds.findIndex((feed) => feed.feedId === Number(params.feedId));
      const existing = feeds[index];
      if (!existing)
        return HttpResponse.json(
          { status: 'error', code: 'FEED_NOT_FOUND', message: '피드를 찾을 수 없습니다.' },
          { status: 404 },
        );
      if (existing.author.handle !== 'crew0')
        return HttpResponse.json(
          {
            status: 'error',
            code: 'FEED_AUTHOR_FORBIDDEN',
            message: '피드 작성자만 요청할 수 있습니다.',
          },
          { status: 403 },
        );
      const body = (await request.json()) as {
        content: string;
        categoryIds: number[];
        mediaIds: number[];
      };
      if (
        !body.content?.trim() ||
        Array.from(body.content).length > 500 ||
        !Array.isArray(body.categoryIds) ||
        body.categoryIds.filter((id) => id === 1).length !== 1 ||
        body.categoryIds.some((id) => id !== 1 && id !== 3) ||
        new Set(body.categoryIds).size !== body.categoryIds.length ||
        !Array.isArray(body.mediaIds) ||
        new Set(body.mediaIds).size !== body.mediaIds.length
      ) {
        return HttpResponse.json(
          {
            status: 'error',
            code: 'VALIDATION_ERROR',
            message: '본문과 카테고리를 확인해 주세요.',
          },
          { status: 400 },
        );
      }
      const feed: Feed = {
        ...existing,
        content: body.content,
        categories: body.categoryIds.map((id) =>
          id === 1
            ? { categoryId: 1, slug: 'backend', displayName: '백엔드', type: 'GENERAL' }
            : { categoryId: 3, slug: 'tecode-talk', displayName: '테코드톡', type: 'EVENT' },
        ),
        // 조회 응답의 미디어는 공개 URL만 내려준다. mediaId는 요청에만 쓴다.
        media: body.mediaIds.map((mediaId, displayOrder) => ({
          displayOrder,
          url: `https://cdn.example.com/media/${mediaId}`,
        })),
        updatedAt: new Date().toISOString(),
      };
      feeds[index] = feed;
      return HttpResponse.json({ status: 'success', data: feed });
    }),
    http.delete('/api/v1/feeds/:feedId', ({ params }) => {
      const index = feeds.findIndex((feed) => feed.feedId === Number(params.feedId));
      if (index < 0)
        return HttpResponse.json(
          { status: 'error', code: 'FEED_NOT_FOUND', message: '피드를 찾을 수 없습니다.' },
          { status: 404 },
        );
      if (feeds[index]?.author.handle !== 'crew0')
        return HttpResponse.json(
          {
            status: 'error',
            code: 'FEED_AUTHOR_FORBIDDEN',
            message: '피드 작성자만 요청할 수 있습니다.',
          },
          { status: 403 },
        );
      feeds.splice(index, 1);
      return new HttpResponse(null, { status: 204 });
    }),
    http.get('/api/v1/feeds/:feedId', ({ params }) => {
      const feed = feeds.find((item) => item.feedId === Number(params.feedId));
      return feed
        ? HttpResponse.json({ status: 'success', data: feed })
        : HttpResponse.json(
            { status: 'error', code: 'FEED_NOT_FOUND', message: '피드를 찾을 수 없습니다.' },
            { status: 404 },
          );
    }),
    http.get('/api/v1/feeds', ({ request }) => {
      const url = new URL(request.url);
      const offset = Number(url.searchParams.get('cursor') ?? 0);
      const data =
        url.searchParams.get('categoryId') && url.searchParams.get('categoryId') !== '1'
          ? []
          : url.searchParams.get('sort') === 'POPULAR'
            ? [...feeds].reverse()
            : feeds;
      const end = offset + Math.min(Number(url.searchParams.get('size') ?? 20), 3);
      return HttpResponse.json({
        status: 'success',
        data: data.slice(offset, end),
        meta: { nextCursor: end < data.length ? String(end) : null, hasNext: end < data.length },
      });
    }),
    http.get('/api/v1/auth/session', () =>
      HttpResponse.json({
        status: 'success',
        data: { status: 'AUTHENTICATED', userId: 1, role: 'USER', csrfToken: 'development-token' },
      }),
    ),
    http.get('/api/v1/feeds/:feedId/comments', ({ params, request }) => {
      const url = new URL(request.url);
      const items = [...getComments(Number(params.feedId))].sort(
        (a, b) => a.createdAt.localeCompare(b.createdAt) || a.id - b.id,
      );
      if (url.searchParams.get('sort') !== 'OLDEST') items.reverse();
      const start = Number(url.searchParams.get('cursor') ?? 0);
      const end = start + 20;
      return HttpResponse.json({
        status: 'success',
        data: items.slice(start, end),
        meta: { nextCursor: end < items.length ? String(end) : null, hasNext: end < items.length },
      });
    }),
    http.post('/api/v1/feeds/:feedId/comments', async ({ params, request }) => {
      const body = (await request.json()) as { content: string };
      if (!body.content?.trim())
        return HttpResponse.json(
          { status: 'error', code: 'VALIDATION_ERROR', message: '댓글 내용을 확인해 주세요.' },
          { status: 400 },
        );
      const item: FeedComment = {
        id: sequence++,
        content: body.content,
        author: { userId: 1, displayName: '개발용 사용자', avatarUrl: null },
        parentId: null,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        editable: true,
        edited: false,
        deleted: false,
      };
      getComments(Number(params.feedId)).push(item);
      return HttpResponse.json({ status: 'success', data: item }, { status: 201 });
    }),
    http.patch('/api/v1/feeds/:feedId/comments/:commentId', async ({ params, request }) => {
      const item = getComments(Number(params.feedId)).find(
        (item) => item.id === Number(params.commentId),
      );
      if (!item)
        return HttpResponse.json(
          { status: 'error', code: 'COMMENT_NOT_FOUND', message: '댓글을 찾을 수 없습니다.' },
          { status: 404 },
        );
      const body = (await request.json()) as { content: string };
      Object.assign(item, {
        content: body.content,
        edited: true,
        updatedAt: new Date().toISOString(),
      });
      return HttpResponse.json({ status: 'success', data: item });
    }),
    http.delete('/api/v1/feeds/:feedId/comments/:commentId', ({ params }) => {
      const items = getComments(Number(params.feedId));
      const item = items.find((item) => item.id === Number(params.commentId));
      if (!item)
        return HttpResponse.json(
          { status: 'error', code: 'COMMENT_NOT_FOUND', message: '댓글을 찾을 수 없습니다.' },
          { status: 404 },
        );
      Object.assign(item, { content: null, deleted: true, editable: false });
      return HttpResponse.json({
        status: 'success',
        data: { id: Number(params.commentId), deleted: true },
      });
    }),
  ];
}
