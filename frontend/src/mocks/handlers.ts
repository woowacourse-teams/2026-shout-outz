import { http, HttpResponse } from 'msw';
import type { Feed } from '@/apis/feed';
import type { FeedComment } from '@/apis/feed-comment';
export const mockFeeds: Feed[] = Array.from({ length: 6 }, (_, index) => ({
  feedId: index + 1,
  title: ['WebSocket 동기화 개선기', 'TanStack Query 서버 상태 관리', '기억에 남는 트러블슈팅'][
    index % 3
  ]!,
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
export function createFeedHandlers({ includeProfile = true }: { includeProfile?: boolean } = {}) {
  let sequence = 100;
  const feeds = [...mockFeeds];
  const comments = new Map<number, FeedComment[]>();
  const feedLikes = new Map(
    feeds.map((feed) => [feed.feedId, { likeCount: 12, likedByMe: false }]),
  );
  const getComments = (id: number) => {
    if (!comments.has(id))
      comments.set(id, [
        {
          id: id * 10,
          content: '경험을 공유해 주셔서 감사합니다!',
          author: {
            userId: 1,
            handle: 'woojin',
            displayName: '개발용 사용자',
            userType: 'WOOWACOURSE_CREW',
            cohort: 8,
            track: 'BACKEND',
            avatarUrl: null,
          },
          parentId: null,
          createdAt: '2026-09-14T00:00:00Z',
          updatedAt: '2026-09-14T00:00:00Z',
          editable: true,
          edited: false,
          deleted: false,
          agreeCount: 2,
          agreedByMe: false,
        },
      ]);
    return comments.get(id)!;
  };
  const withFeedState = (feed: Feed) => ({
    ...feed,
    ...feedLikes.get(feed.feedId),
    commentCount: getComments(feed.feedId).filter((comment) => !comment.deleted).length,
  });
  const setCommentReaction = (feedId: number, commentId: number, active: boolean) => {
    const comment = getComments(feedId).find((item) => item.id === commentId);
    if (!comment) return HttpResponse.json({ status: 'error' }, { status: 404 });
    const wasActive = comment.agreedByMe ?? false;
    if (wasActive !== active) {
      comment.agreedByMe = active;
      comment.agreeCount = Math.max(0, (comment.agreeCount ?? 0) + (active ? 1 : -1));
    }
    return HttpResponse.json({
      status: 'success',
      data: { commentId, type: 'AGREE', active, agreeCount: comment.agreeCount ?? 0 },
    });
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
    ...(includeProfile
      ? [
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
        ]
      : []),
    http.post('/api/v1/feeds', async ({ request }) => {
      const body = (await request.json()) as {
        title: string;
        content: string;
        categoryIds: number[];
        mediaIds: number[];
      };
      if (
        !body.title?.trim() ||
        Array.from(body.title).length > 100 ||
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
            message: '제목과 본문, 카테고리를 확인해 주세요.',
          },
          { status: 400 },
        );
      }
      const feed: Feed = {
        ...mockFeeds[0]!,
        feedId: sequence++,
        title: body.title,
        content: body.content,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      feeds.unshift(feed);
      feedLikes.set(feed.feedId, { likeCount: 0, likedByMe: false });
      return HttpResponse.json({ status: 'success', data: withFeedState(feed) }, { status: 201 });
    }),
    ...(includeProfile
      ? [
          http.get('/api/v1/users/me/summary', () =>
            HttpResponse.json({
              status: 'success',
              data: { handle: 'crew0', displayName: '정우진', avatarUrl: null },
            }),
          ),
        ]
      : []),
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
        title: string;
        content: string;
        categoryIds: number[];
        mediaIds: number[];
      };
      if (
        !body.title?.trim() ||
        Array.from(body.title).length > 100 ||
        !body.content?.trim() ||
        Array.from(body.content).length > 500 ||
        !Array.isArray(body.categoryIds) ||
        body.categoryIds.filter((id) => id === 1).length !== 1 ||
        body.categoryIds.some((id) => id !== 1 && id !== 3) ||
        new Set(body.categoryIds).size !== body.categoryIds.length ||
        !Array.isArray(body.mediaIds) ||
        new Set(body.mediaIds).size !== body.mediaIds.length ||
        body.mediaIds.some((id) => !existing.media.some((media) => media.mediaId === id))
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
        title: body.title,
        content: body.content,
        categories: body.categoryIds.map((id) =>
          id === 1
            ? { categoryId: 1, slug: 'backend', displayName: '백엔드', type: 'GENERAL' }
            : { categoryId: 3, slug: 'tecode-talk', displayName: '테코드톡', type: 'EVENT' },
        ),
        media: body.mediaIds.map((mediaId, displayOrder) => ({
          displayOrder,
          mediaId,
          url: `https://cdn.example.com/media/${mediaId}`,
        })),
        updatedAt: new Date().toISOString(),
      };
      feeds[index] = feed;
      return HttpResponse.json({ status: 'success', data: withFeedState(feed) });
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
    http.put('/api/v1/feeds/:feedId/reactions/LIKE', ({ params }) => {
      const feedId = Number(params.feedId);
      const feed = feeds.find((item) => item.feedId === feedId);
      if (!feed) return HttpResponse.json({ status: 'error' }, { status: 404 });
      const state = feedLikes.get(feedId) ?? { likeCount: 0, likedByMe: false };
      if (!state.likedByMe) {
        state.likedByMe = true;
        state.likeCount += 1;
      }
      feedLikes.set(feedId, state);
      return HttpResponse.json({
        status: 'success',
        data: { feedId, type: 'LIKE', active: true, likeCount: state.likeCount, bookmarkCount: 0 },
      });
    }),
    http.delete('/api/v1/feeds/:feedId/reactions/LIKE', ({ params }) => {
      const feedId = Number(params.feedId);
      const state = feedLikes.get(feedId) ?? { likeCount: 0, likedByMe: false };
      if (state.likedByMe) {
        state.likedByMe = false;
        state.likeCount = Math.max(0, state.likeCount - 1);
      }
      feedLikes.set(feedId, state);
      return HttpResponse.json({
        status: 'success',
        data: { feedId, type: 'LIKE', active: false, likeCount: state.likeCount, bookmarkCount: 0 },
      });
    }),
    http.get('/api/v1/feeds/:feedId', ({ params }) => {
      const feed = feeds.find((item) => item.feedId === Number(params.feedId));
      return feed
        ? HttpResponse.json({ status: 'success', data: withFeedState(feed) })
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
        data: data.slice(offset, end).map(withFeedState),
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
    http.put('/api/v1/feeds/:feedId/comments/:commentId/reactions/AGREE', ({ params }) =>
      setCommentReaction(Number(params.feedId), Number(params.commentId), true),
    ),
    http.delete('/api/v1/feeds/:feedId/comments/:commentId/reactions/AGREE', ({ params }) =>
      setCommentReaction(Number(params.feedId), Number(params.commentId), false),
    ),
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
        author: {
          userId: 1,
          handle: 'woojin',
          displayName: '개발용 사용자',
          userType: 'WOOWACOURSE_CREW',
          cohort: 8,
          track: 'BACKEND',
          avatarUrl: null,
        },
        parentId: null,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        editable: true,
        edited: false,
        deleted: false,
        agreeCount: 0,
        agreedByMe: false,
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
