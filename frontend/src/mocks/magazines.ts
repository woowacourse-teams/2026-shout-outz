import { http, HttpResponse } from 'msw';
import type { Magazine } from '@/apis/magazine';

const CONTENT_MD = `## 자기소개 부탁드려요

안녕하세요, 우아한테크코스 8기 백엔드 크루 **우진**입니다.

<b color="var(--static-blue-500, #3688FF)">Q. 레벨 3 프로젝트에서 가장 기억에 남는 순간은?</b>

> 처음으로 <span color="red">실제 사용자</span>가 남긴 버그 제보를 받았을 때요.

팀원들과 함께 다음 순서로 문제를 해결했습니다.

1. 제보 내용을 재현한다
2. 원인을 찾고 테스트를 먼저 작성한다
3. 수정 후 제보자에게 결과를 알린다

![프로젝트 회고 사진](https://picsum.photos/seed/magazine-retro/960/540 "팀원들과 레벨 3 회고를 마친 날")

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

export const magazineHandlers = [
  http.get('/api/v1/magazines', () =>
    HttpResponse.json({
      status: 'success',
      // 목록 응답에는 본문(contentMd) 대신 발행 상태가 있다.
      data: mockMagazines.map((magazine) => ({
        ...magazine,
        contentMd: undefined,
        status: 'PUBLISHED',
      })),
      meta: { hasNext: false, nextCursor: null, totalCount: mockMagazines.length },
    }),
  ),
  http.get('/api/v1/magazines/:magazineId', ({ params }) => {
    const magazine = mockMagazines.find((item) => item.id === Number(params.magazineId));
    return magazine
      ? HttpResponse.json({ status: 'success', data: magazine })
      : HttpResponse.json(
          { status: 'error', code: 'MAGAZINE_NOT_FOUND', message: '인터뷰를 찾을 수 없습니다.' },
          { status: 404 },
        );
  }),
];

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

const notFound = () =>
  HttpResponse.json(
    { status: 'error', code: 'MAGAZINE_NOT_FOUND', message: '인터뷰를 찾을 수 없습니다.' },
    { status: 404 },
  );

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
