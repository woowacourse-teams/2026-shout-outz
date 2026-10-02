import { http, HttpResponse } from 'msw';
import type { Feed } from '@/apis/feed';
import type { FeedComment } from '@/apis/feed-comment';
import { findFirstUrl } from '@/utils/feed';

/** 서버처럼 본문 첫 URL로 링크 미리보기를 만든다. 작성·수정 직후는 OG 수집 전이라 url만 채운다. */
const linkPreviewOf = (content: string): Feed['linkPreview'] => {
  const url = findFirstUrl(content);
  return url ? { url, title: null, description: null, imageUrl: null, siteName: null } : undefined;
};
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
  isAnonymous: index === 0 || index === 2,
  feedType: index === 2 || index === 5 ? 'QUESTION' : 'POST',
  author: {
    userId: index + 1,
    handle: `crew${index}`,
    displayName: ['정우진', '김도현', '이지민'][index % 3]!,
    userType: 'WOOWACOURSE_CREW',
    track: index % 2 ? 'FRONTEND' : 'BACKEND',
    cohort: index === 0 ? 8 : 6,
    isCurrent: index === 0,
    avatarUrl: null,
  },
  categories:
    index === 2 || index === 5
      ? [
          {
            categoryId: 2,
            slug: 'career',
            displayName: '진로 고민',
            type: 'GENERAL',
            feedType: 'QUESTION',
          },
        ]
      : [
          {
            categoryId: 1,
            slug: 'backend',
            displayName: '개발 이야기',
            type: 'GENERAL',
            feedType: 'POST',
          },
        ],
  linkPreview:
    index % 3 === 0
      ? {
          url: 'https://woojin.log/tech/redis-pub-sub',
          title: '여러 서버의 WebSocket 세션 묶기',
          description:
            '여러 서버에 흩어진 WebSocket 세션을 Redis Pub/Sub으로 묶은 과정을 정리했습니다.',
          // 첫 피드는 OG 이미지가 있는 경우, 네 번째 피드는 없는 경우를 보여준다.
          imageUrl: index === 0 ? 'https://picsum.photos/seed/redis-pub-sub/1200/630' : null,
          siteName: 'woojin.log',
        }
      : undefined,
  media: [],
  likeCount: 0,
  likedByMe: false,
  bookmarkCount: 0,
  bookmarkedByMe: false,
  commentCount: 0,
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
    if (!comments.has(id)) {
      const items: FeedComment[] = [];
      if (id !== 6) {
        items.push({
          id: id * 10,
          content: '경험을 공유해 주셔서 감사합니다!',
          author: {
            userId: 1,
            handle: 'woojin',
            displayName: '개발용 사용자',
            userType: 'WOOWACOURSE_CREW',
            isCurrent: true,
            cohort: 8,
            track: 'BACKEND',
            avatarUrl: null,
          },
          parentId: null,
          isAnonymous: false,
          createdAt: '2026-09-14T00:00:00Z',
          updatedAt: '2026-09-14T00:00:00Z',
          editable: true,
          edited: false,
          deleted: false,
          agreeCount: 2,
          agreedByMe: false,
        });
      }
      if (id === 3) {
        items.unshift({
          id: id * 10 + 1,
          content: '이 댓글은 익명으로 작성했습니다.',
          author: {
            userId: null,
            handle: null,
            displayName: null,
            userType: 'WOOWACOURSE_CREW',
            isCurrent: false,
            track: null,
            avatarUrl: null,
          } as unknown as FeedComment['author'],
          parentId: null,
          createdAt: '2026-09-14T01:00:00Z',
          updatedAt: '2026-09-14T01:00:00Z',
          editable: false,
          edited: false,
          deleted: false,
          agreeCount: 1,
          agreedByMe: false,
          isAnonymous: true,
        });
        items.push({
          id: id * 10 + 2,
          content: '저도 같은 경험이 있어요.',
          author: {
            userId: 2,
            handle: 'crew1',
            displayName: '김도현',
            userType: 'WOOWACOURSE_CREW',
            isCurrent: false,
            track: 'FRONTEND',
            avatarUrl: null,
          },
          parentId: id * 10 + 1,
          createdAt: '2026-09-14T02:00:00Z',
          updatedAt: '2026-09-14T02:00:00Z',
          editable: false,
          edited: false,
          deleted: false,
          agreeCount: 0,
          agreedByMe: false,
          isAnonymous: false,
        });
      }
      comments.set(id, items);
    }
    return comments.get(id)!;
  };
  const withFeedState = (feed: Feed) => ({
    ...feed,
    author:
      feed.isAnonymous && feed.author.userId !== 1
        ? ({
            userId: null,
            handle: null,
            displayName: null,
            userType: feed.author.userType,
            track: null,
            cohort: null,
            isCurrent: feed.author.isCurrent,
            avatarUrl: null,
          } as unknown as Feed['author'])
        : feed.author,
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
    http.get('/api/v1/categories', ({ request }) =>
      HttpResponse.json({
        status: 'success',
        data: (
          [
            {
              categoryId: 1,
              slug: 'backend',
              displayName: '백엔드',
              type: 'GENERAL',
              feedType: 'POST',
              displayOrder: 1,
            },
            {
              categoryId: 3,
              slug: 'tecode-talk',
              displayName: '테코드톡',
              type: 'EVENT',
              feedType: 'POST',
              displayOrder: 2,
            },
            {
              categoryId: 2,
              slug: 'career',
              displayName: '진로 고민',
              type: 'GENERAL',
              feedType: 'QUESTION',
              displayOrder: 1,
            },
            {
              categoryId: 4,
              slug: 'question-event',
              displayName: '질문 이벤트',
              type: 'EVENT',
              feedType: 'QUESTION',
              displayOrder: 2,
            },
          ] as const
        ).filter(
          (category) =>
            !new URL(request.url).searchParams.has('feedType') ||
            category.feedType === new URL(request.url).searchParams.get('feedType'),
        ),
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
        isAnonymous?: boolean;
        feedType?: 'POST' | 'QUESTION';
      };
      if (
        !body.title?.trim() ||
        Array.from(body.title).length > 100 ||
        !body.content?.trim() ||
        Array.from(body.content).length > 5000 ||
        body.categoryIds?.length !== 1 ||
        body.categoryIds[0] !== (body.feedType === 'QUESTION' ? 2 : 1) ||
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
        linkPreview: linkPreviewOf(body.content),
        isAnonymous: body.isAnonymous ?? false,
        feedType: body.feedType ?? 'POST',
        categories:
          body.feedType === 'QUESTION'
            ? [
                {
                  categoryId: 2,
                  slug: 'career',
                  displayName: '진로 고민',
                  type: 'GENERAL',
                  feedType: 'QUESTION',
                },
              ]
            : [
                {
                  categoryId: 1,
                  slug: 'backend',
                  displayName: '백엔드',
                  type: 'GENERAL',
                  feedType: 'POST',
                },
              ],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      feeds.unshift(feed);
      feedLikes.set(feed.feedId, { likeCount: 0, likedByMe: false });
      return HttpResponse.json({ status: 'success', data: withFeedState(feed) }, { status: 201 });
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
        body.categoryIds.filter((id) => id === (existing.feedType === 'QUESTION' ? 2 : 1))
          .length !== 1 ||
        body.categoryIds.some(
          (id) => !(existing.feedType === 'QUESTION' ? [2, 4] : [1, 3]).includes(id),
        ) ||
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
        linkPreview: linkPreviewOf(body.content),
        categories: body.categoryIds.map((id) =>
          id === 1
            ? {
                categoryId: 1,
                slug: 'backend',
                displayName: '백엔드',
                type: 'GENERAL',
                feedType: 'POST',
              }
            : id === 2
              ? {
                  categoryId: 2,
                  slug: 'career',
                  displayName: '진로 고민',
                  type: 'GENERAL',
                  feedType: 'QUESTION',
                }
              : id === 3
                ? {
                    categoryId: 3,
                    slug: 'tecode-talk',
                    displayName: '테코드톡',
                    type: 'EVENT',
                    feedType: 'POST',
                  }
                : {
                    categoryId: 4,
                    slug: 'question-event',
                    displayName: '질문 이벤트',
                    type: 'EVENT',
                    feedType: 'QUESTION',
                  },
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
        url.searchParams.get('categoryId') &&
        !['1', '2'].includes(url.searchParams.get('categoryId')!)
          ? []
          : feeds
              .filter((feed) => {
                const type = url.searchParams.get('type');
                const keyword = url.searchParams.get('keyword')?.toLocaleLowerCase();
                const waiting = url.searchParams.get('sort') === 'WAITING';
                return (
                  (!type || feed.feedType === type) &&
                  (!waiting ||
                    (feed.feedType === 'QUESTION' && getComments(feed.feedId).length === 0)) &&
                  (!keyword ||
                    `${feed.title} ${feed.content}`.toLocaleLowerCase().includes(keyword))
                );
              })
              .sort((a, b) =>
                url.searchParams.get('sort') === 'POPULAR' ? b.feedId - a.feedId : 0,
              );
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
      const all = getComments(Number(params.feedId));
      const roots = all
        .filter((item) => item.parentId == null)
        .sort((a, b) => a.createdAt.localeCompare(b.createdAt) || a.id - b.id);
      if (url.searchParams.get('sort') !== 'OLDEST') roots.reverse();
      const start = Number(url.searchParams.get('cursor') ?? 0);
      const end = start + Math.max(1, Number(url.searchParams.get('size') ?? 20));
      const items = roots
        .slice(start, end)
        .flatMap((root) => [
          root,
          ...all
            .filter((item) => item.parentId === root.id)
            .sort((a, b) => a.createdAt.localeCompare(b.createdAt) || a.id - b.id),
        ]);
      return HttpResponse.json({
        status: 'success',
        data: items,
        meta: { nextCursor: end < roots.length ? String(end) : null, hasNext: end < roots.length },
      });
    }),
    http.put('/api/v1/feeds/:feedId/comments/:commentId/reactions/AGREE', ({ params }) =>
      setCommentReaction(Number(params.feedId), Number(params.commentId), true),
    ),
    http.delete('/api/v1/feeds/:feedId/comments/:commentId/reactions/AGREE', ({ params }) =>
      setCommentReaction(Number(params.feedId), Number(params.commentId), false),
    ),
    http.post('/api/v1/feeds/:feedId/comments', async ({ params, request }) => {
      const body = (await request.json()) as {
        content: string;
        isAnonymous?: boolean;
        parentId?: number | null;
      };
      if (!body.content?.trim())
        return HttpResponse.json(
          { status: 'error', code: 'VALIDATION_ERROR', message: '댓글 내용을 확인해 주세요.' },
          { status: 400 },
        );
      const parent =
        body.parentId == null
          ? null
          : getComments(Number(params.feedId)).find((item) => item.id === body.parentId);
      if (body.parentId != null && (!parent || parent.parentId != null || parent.deleted))
        return HttpResponse.json(
          {
            status: 'error',
            code: 'COMMENT_PARENT_INVALID',
            message: '답글을 달 수 없는 댓글입니다.',
          },
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
          isCurrent: true,
          cohort: 8,
          track: 'BACKEND',
          avatarUrl: null,
        },
        parentId: body.parentId ?? null,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        editable: true,
        edited: false,
        deleted: false,
        agreeCount: 0,
        agreedByMe: false,
        isAnonymous: body.isAnonymous ?? false,
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
      const body = (await request.json()) as { content: string; isAnonymous?: boolean };
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
