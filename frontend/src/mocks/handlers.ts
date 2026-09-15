import { http, HttpResponse } from 'msw';
import type { Feed } from '@/apis/feed';
import type { FeedComment } from '@/apis/feed-comment';
export const mockFeeds: Feed[] = Array.from({ length: 6 }, (_, index) => ({
  postId: index + 1,
  content: [
    'Redis Pub/Sub으로 WebSocket 동기화 지연을 개선한 경험을 공유합니다.\n\n캐시 무효화와 메시지 순서를 함께 고민했어요.',
    'TanStack Query를 사용하면서 배운 서버 상태 관리 이야기입니다.\n\n```ts\nawait queryClient.invalidateQueries({ queryKey: ["projects"] });\n```',
    '프로젝트에서 가장 기억에 남는 트러블슈팅은 무엇인가요?',
  ][index % 3]!,
  author: {
    handle: `crew${index}`,
    displayName: ['정우진', '김도현', '이지민'][index % 3]!,
    userType: 'WOOWACOURSE_CREW',
    track: index % 2 ? 'FRONTEND' : 'BACKEND',
    cohort: 6,
    avatarImageId: null,
  },
  categories: [{ categoryId: 1, slug: 'backend', displayName: '개발 이야기', type: 'GENERAL' }],
  media: [],
  createdAt: new Date(Date.UTC(2026, 8, 14, 9 - index)).toISOString(),
  updatedAt: new Date(Date.UTC(2026, 8, 14, 9 - index)).toISOString(),
}));
export function createFeedHandlers() {
  let sequence = 100;
  const comments = new Map<number, FeedComment[]>();
  const getComments = (id: number) => {
    if (!comments.has(id))
      comments.set(id, [
        {
          id: id * 10,
          content: '경험을 공유해 주셔서 감사합니다!',
          author: { userId: 1, name: '개발용 사용자', avatarUrl: null },
          parentId: null,
          createdAt: '2026-09-14T00:00:00Z',
          updatedAt: '2026-09-14T00:00:00Z',
          editable: true,
        },
      ]);
    return comments.get(id)!;
  };
  return [
    http.get('/api/v1/posts/:postId', ({ params }) => {
      const feed = mockFeeds.find((item) => item.postId === Number(params.postId));
      return feed
        ? HttpResponse.json({ status: 'success', data: feed })
        : HttpResponse.json(
            { status: 'error', code: 'POST_NOT_FOUND', message: '피드를 찾을 수 없습니다.' },
            { status: 404 },
          );
    }),
    http.get('/api/v1/posts', ({ request }) => {
      const url = new URL(request.url);
      const offset = Number(url.searchParams.get('cursor') ?? 0);
      const data =
        url.searchParams.get('categoryId') && url.searchParams.get('categoryId') !== '1'
          ? []
          : url.searchParams.get('sort') === 'POPULAR'
            ? [...mockFeeds].reverse()
            : mockFeeds;
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
    http.get('/api/v1/posts/:postId/comments', ({ params, request }) => {
      const url = new URL(request.url);
      const items = [...getComments(Number(params.postId))].sort(
        (a, b) => a.createdAt.localeCompare(b.createdAt) || a.id - b.id,
      );
      if (url.searchParams.get('sort') !== 'OLDEST') items.reverse();
      const start = Number(url.searchParams.get('cursor') ?? 0);
      const end = start + 20;
      return HttpResponse.json({
        status: 'success',
        data: { items: items.slice(start, end) },
        meta: { nextCursor: end < items.length ? String(end) : null, hasNext: end < items.length },
      });
    }),
    http.post('/api/v1/posts/:postId/comments', async ({ params, request }) => {
      const body = (await request.json()) as { content: string };
      if (!body.content?.trim())
        return HttpResponse.json(
          { status: 'error', code: 'VALIDATION_ERROR', message: '댓글 내용을 확인해 주세요.' },
          { status: 400 },
        );
      const item: FeedComment = {
        id: sequence++,
        content: body.content,
        author: { userId: 1, name: '개발용 사용자', avatarUrl: null },
        parentId: null,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        editable: true,
      };
      getComments(Number(params.postId)).push(item);
      return HttpResponse.json({ status: 'success', data: item }, { status: 201 });
    }),
    http.patch('/api/v1/posts/:postId/comments/:commentId', async ({ params, request }) => {
      const item = getComments(Number(params.postId)).find(
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
    http.delete('/api/v1/posts/:postId/comments/:commentId', ({ params }) => {
      const items = getComments(Number(params.postId));
      const index = items.findIndex((item) => item.id === Number(params.commentId));
      if (index < 0)
        return HttpResponse.json(
          { status: 'error', code: 'COMMENT_NOT_FOUND', message: '댓글을 찾을 수 없습니다.' },
          { status: 404 },
        );
      items.splice(index, 1);
      return HttpResponse.json({
        status: 'success',
        data: { id: Number(params.commentId), deleted: true },
      });
    }),
  ];
}
