import { http, HttpResponse } from 'msw';
import type { Magazine } from '@/apis/magazine';
import type { FeedComment } from '@/apis/feed-comment';

const CONTENT_MD = `## 자기소개 부탁드려요

안녕하세요, 우아한테크코스 8기 백엔드 크루 **우진**입니다.

<b color="blue">Q. 레벨 3 프로젝트에서 가장 기억에 남는 순간은?</b>

> 처음으로 <span color="red">실제 사용자</span>가 남긴 버그 제보를 받았을 때요.

팀원들과 함께 다음 순서로 문제를 해결했습니다.


1. 제보 내용을 재현한다
2. 원인을 찾고 테스트를 먼저 작성한다
3. 수정 후 제보자에게 결과를 알린다

![프로젝트 회고 사진|480](https://picsum.photos/seed/magazine-retro/960/540 "팀원들과 레벨 3 회고를 마친 날")

## 후배 크루에게 한마디

- 작은 기록이라도 <font color="primary">꾸준히</font> 남기세요
- 모르는 건 \`질문\`하는 게 가장 빠릅니다
`;

export const mockMagazines: Magazine[] = Array.from({ length: 4 }, (_, index) => ({
  id: index + 1,
  title: [
    '우진의 레벨 3 회고',
    '프론트엔드 크루의 하루',
    '첫 배포를 마친 팀 이야기',
    '코드 리뷰로 배운 것들',
  ][index]!,
  summary: '실제 사용자를 만나며 배운 점을 들어봤어요.',
  thumbnailUrl: index === 3 ? null : `https://picsum.photos/seed/magazine-${index}/960/540`,
  featuredUser: {
    handle: `crew${index + 1}`,
    displayName: ['우진', '서연', '도윤', '하린'][index]!,
    introduction: '우아한테크코스 8기',
    avatarUrl: null,
  },
  author: { handle: '@zzaekkii', displayName: '재키', avatarUrl: null },
  contentMd: CONTENT_MD,
  publishedAt: new Date(Date.UTC(2026, 9, 15 - index * 7)).toISOString(),
  createdAt: new Date(Date.UTC(2026, 9, 15 - index * 7)).toISOString(),
  updatedAt: new Date(Date.UTC(2026, 9, 15 - index * 7)).toISOString(),
  likeCount: 8 - index,
  likedByMe: index === 0,
  commentCount: index,
  viewCount: 120 - index * 10,
}));

const COMMENT_AUTHOR = {
  userId: 1,
  handle: 'woojin',
  displayName: '개발용 사용자',
  userType: 'WOOWACOURSE_CREW',
  isCurrent: true,
  cohort: 8,
  track: 'BACKEND',
  avatarUrl: null,
} satisfies FeedComment['author'];

const notFound = () =>
  HttpResponse.json(
    { status: 'error', code: 'MAGAZINE_NOT_FOUND', message: '매거진을 찾을 수 없습니다.' },
    { status: 404 },
  );

/** 좋아요와 댓글을 핸들러마다 따로 기억한다. 테스트마다 새로 만들어 상태가 넘어가지 않게 한다. */
export function createMagazineHandlers() {
  let sequence = 1000;
  const likes = new Map(
    mockMagazines.map((magazine) => [
      magazine.id,
      { likeCount: magazine.likeCount, likedByMe: magazine.likedByMe ?? false },
    ]),
  );
  const comments = new Map<number, FeedComment[]>(
    mockMagazines.map((magazine) => [
      magazine.id,
      Array.from({ length: magazine.commentCount }, (_, index) => ({
        id: magazine.id * 100 + index,
        content: '좋은 인터뷰 감사합니다!',
        author: COMMENT_AUTHOR,
        parentId: null,
        isAnonymous: false,
        createdAt: '2026-10-15T00:00:00Z',
        updatedAt: '2026-10-15T00:00:00Z',
        editable: true,
        edited: false,
        deleted: false,
        agreeCount: 0,
        agreedByMe: false,
      })),
    ]),
  );
  const withState = (magazine: Magazine) => ({
    ...magazine,
    ...likes.get(magazine.id),
    commentCount: (comments.get(magazine.id) ?? []).filter((item) => !item.deleted).length,
  });
  const findComment = (magazineId: number, commentId: number) =>
    comments.get(magazineId)?.find((item) => item.id === commentId);
  const setLike = (magazineId: number, active: boolean) => {
    const state = likes.get(magazineId);
    if (!state) return notFound();
    if (state.likedByMe !== active) {
      state.likedByMe = active;
      state.likeCount = Math.max(0, state.likeCount + (active ? 1 : -1));
    }
    return HttpResponse.json({
      status: 'success',
      data: { magazineId, type: 'LIKE', active, likeCount: state.likeCount },
    });
  };
  const setAgree = (magazineId: number, commentId: number, active: boolean) => {
    const item = findComment(magazineId, commentId);
    if (!item) return notFound();
    if (item.agreedByMe !== active) {
      item.agreedByMe = active;
      item.agreeCount = Math.max(0, (item.agreeCount ?? 0) + (active ? 1 : -1));
    }
    return HttpResponse.json({
      status: 'success',
      data: { magazineId, commentId, type: 'AGREE', active, agreeCount: item.agreeCount },
    });
  };

  return [
    http.get('/api/v1/magazines', () =>
      HttpResponse.json({
        status: 'success',
        // 목록 응답에는 본문(contentMd) 대신 발행 상태가 있다.
        data: mockMagazines.map((magazine) => ({
          ...withState(magazine),
          contentMd: undefined,
          status: 'PUBLISHED',
        })),
        meta: { hasNext: false, nextCursor: null, totalCount: mockMagazines.length },
      }),
    ),
    http.get('/api/v1/magazines/:magazineId', ({ params }) => {
      const magazine = mockMagazines.find((item) => item.id === Number(params.magazineId));
      return magazine
        ? HttpResponse.json({ status: 'success', data: withState(magazine) })
        : notFound();
    }),
    http.put('/api/v1/magazines/:magazineId/reactions/LIKE', ({ params }) =>
      setLike(Number(params.magazineId), true),
    ),
    http.delete('/api/v1/magazines/:magazineId/reactions/LIKE', ({ params }) =>
      setLike(Number(params.magazineId), false),
    ),
    http.get('/api/v1/magazines/:magazineId/comments', ({ params }) => {
      const items = comments.get(Number(params.magazineId));
      if (!items) return notFound();
      return HttpResponse.json({
        status: 'success',
        data: items,
        meta: {
          hasNext: false,
          nextCursor: null,
          totalCount: items.filter((item) => !item.deleted).length,
        },
      });
    }),
    http.post('/api/v1/magazines/:magazineId/comments', async ({ params, request }) => {
      const items = comments.get(Number(params.magazineId));
      if (!items) return notFound();
      const body = (await request.json()) as {
        content: string;
        isAnonymous?: boolean;
        parentId?: number | null;
      };
      const now = new Date().toISOString();
      const item: FeedComment = {
        id: sequence++,
        content: body.content.trim(),
        author: COMMENT_AUTHOR,
        parentId: body.parentId ?? null,
        isAnonymous: body.isAnonymous ?? false,
        createdAt: now,
        updatedAt: now,
        editable: true,
        edited: false,
        deleted: false,
        agreeCount: 0,
        agreedByMe: false,
      };
      items.push(item);
      return HttpResponse.json({ status: 'success', data: item }, { status: 201 });
    }),
    http.patch('/api/v1/magazines/:magazineId/comments/:commentId', async ({ params, request }) => {
      const item = findComment(Number(params.magazineId), Number(params.commentId));
      if (!item) return notFound();
      const body = (await request.json()) as { content: string };
      Object.assign(item, {
        content: body.content.trim(),
        edited: true,
        updatedAt: new Date().toISOString(),
      });
      return HttpResponse.json({ status: 'success', data: item });
    }),
    http.delete('/api/v1/magazines/:magazineId/comments/:commentId', ({ params }) => {
      const item = findComment(Number(params.magazineId), Number(params.commentId));
      if (!item) return notFound();
      Object.assign(item, { content: null, deleted: true, editable: false });
      return HttpResponse.json({ status: 'success', data: { id: item.id, deleted: true } });
    }),
    http.put('/api/v1/magazines/:magazineId/comments/:commentId/reactions/AGREE', ({ params }) =>
      setAgree(Number(params.magazineId), Number(params.commentId), true),
    ),
    http.delete('/api/v1/magazines/:magazineId/comments/:commentId/reactions/AGREE', ({ params }) =>
      setAgree(Number(params.magazineId), Number(params.commentId), false),
    ),
  ];
}

/** 관리자 목록에서 마지막 인터뷰는 아직 발행하지 않은 초안으로 보여준다. */
const adminStatusOf = (magazine: Magazine) =>
  magazine.id === mockMagazines.length ? 'DRAFT' : 'PUBLISHED';

/** 관리자 상세는 대상 ID와 이미지 원본 참조(`media://ID`)가 담긴 본문을 준다. */
const toAdminMagazine = (magazine: Magazine) => ({
  id: magazine.id,
  featuredUserId: 100 + magazine.id,
  featuredUserIntroduction: magazine.featuredUser?.introduction ?? null,
  author: magazine.author,
  title: magazine.title,
  summary: magazine.summary,
  thumbnailMediaId: magazine.thumbnailUrl ? 500 + magazine.id : null,
  thumbnailUrl: magazine.thumbnailUrl,
  contentMd: magazine.contentMd,
  previewContentMd: magazine.contentMd,
  status: adminStatusOf(magazine),
  publishedAt: adminStatusOf(magazine) === 'PUBLISHED' ? magazine.publishedAt : null,
  createdAt: magazine.createdAt,
  updatedAt: magazine.updatedAt,
});

const findMagazine = (magazineId: unknown) =>
  mockMagazines.find((item) => item.id === Number(magazineId));

export const adminMagazineHandlers = [
  http.get('/api/v1/admin/magazines', () =>
    HttpResponse.json({
      status: 'success',
      data: mockMagazines.map((magazine) => ({
        ...magazine,
        contentMd: undefined,
        status: adminStatusOf(magazine),
      })),
      meta: { hasNext: false, nextCursor: null, totalCount: mockMagazines.length },
    }),
  ),
  http.post('/api/v1/admin/magazines', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    const now = new Date().toISOString();
    return HttpResponse.json(
      {
        status: 'success',
        data: {
          ...body,
          id: 99,
          status: 'DRAFT',
          publishedAt: null,
          createdAt: now,
          updatedAt: now,
        },
      },
      { status: 201 },
    );
  }),
  http.get('/api/v1/admin/magazines/:magazineId', ({ params }) => {
    const magazine = findMagazine(params.magazineId);
    return magazine
      ? HttpResponse.json({ status: 'success', data: toAdminMagazine(magazine) })
      : notFound();
  }),
  http.put('/api/v1/admin/magazines/:magazineId', async ({ params, request }) => {
    const magazine = findMagazine(params.magazineId);
    if (!magazine) return notFound();
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json({
      status: 'success',
      data: { ...toAdminMagazine(magazine), ...body },
    });
  }),
  http.delete('/api/v1/admin/magazines/:magazineId', ({ params }) =>
    findMagazine(params.magazineId) ? new HttpResponse(null, { status: 204 }) : notFound(),
  ),
  http.post('/api/v1/admin/magazines/:magazineId/publish', ({ params }) => {
    const magazine = findMagazine(params.magazineId);
    return magazine
      ? HttpResponse.json({
          status: 'success',
          data: { ...toAdminMagazine(magazine), status: 'PUBLISHED' },
        })
      : notFound();
  }),
  http.post('/api/v1/admin/magazines/:magazineId/unpublish', ({ params }) => {
    const magazine = findMagazine(params.magazineId);
    return magazine
      ? HttpResponse.json({
          status: 'success',
          data: { ...toAdminMagazine(magazine), status: 'DRAFT', publishedAt: null },
        })
      : notFound();
  }),
];
