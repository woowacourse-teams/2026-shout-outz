/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { http, HttpResponse } from 'msw';
import { Route as MagazineDetailRoute } from '@/routes/magazines/$magazineId';
import { createMagazineHandlers } from '@/mocks/magazines';
import { renderRoute, server } from '@/test/renderRoute';

beforeEach(() => server.use(...createMagazineHandlers()));

it('매거진 본문을 마크다운으로 그린다', async () => {
  renderRoute('/magazines/1');

  expect(
    await screen.findByRole('heading', { level: 1, name: '우진의 레벨 3 회고' }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('heading', { level: 2, name: '자기소개 부탁드려요' }),
  ).toBeInTheDocument();
  expect(screen.getByText('우진', { selector: 'strong' })).toBeInTheDocument();
  expect(screen.getByRole('img', { name: '프로젝트 회고 사진' })).toHaveStyle({
    width: '480px',
    display: 'block',
    marginInline: 'auto',
  });
  expect(screen.getAllByRole('listitem')).toHaveLength(5);
});

it('이미지 title은 캡션으로, 태그의 color는 정해둔 색으로 보여준다', async () => {
  renderRoute('/magazines/1');

  expect(await screen.findByText('팀원들과 레벨 3 회고를 마친 날')).toBeInTheDocument();
  expect(screen.getByText('실제 사용자')).toHaveClass('text-red-600!');
  expect(screen.getByText('꾸준히')).toHaveClass('text-primary-600!');
  expect(screen.getByText('Q. 레벨 3 프로젝트에서 가장 기억에 남는 순간은?')).toHaveClass(
    'text-blue-600!',
  );
});

it('빈 줄을 두 줄 이상 넣으면 넘친 줄만큼 빈 문단으로 띄운다', async () => {
  renderRoute('/magazines/1');

  const paragraph = await screen.findByText('팀원들과 함께 다음 순서로 문제를 해결했습니다.');
  const spacer = paragraph.nextElementSibling;
  expect(spacer?.tagName).toBe('P');
  expect(spacer?.querySelector('br')).toBeInTheDocument();
  expect(spacer).toHaveTextContent('');
  expect(spacer?.nextElementSibling?.tagName).toBe('OL');
});

it('매거진 탭 목록에서 카드를 누르면 상세로 이동한다', async () => {
  renderRoute('/magazines');

  expect(await screen.findByRole('link', { name: '첫 배포를 마친 팀 이야기' })).toHaveAttribute(
    'href',
    '/magazines/3',
  );
});

it('상단에는 에디터를, 본문 앞에는 인터뷰이와 소개문을 보여준다', async () => {
  renderRoute('/magazines/1');

  expect(await screen.findByText('에디터')).toBeInTheDocument();
  expect(screen.getByText('재키')).toBeInTheDocument();

  const intro = screen.getByRole('region', { name: '인터뷰이 소개' });
  expect(within(intro).getByRole('link', { name: '우진 프로필 보기' })).toHaveAttribute(
    'href',
    '/users/crew1',
  );
  expect(within(intro).getByText('우아한테크코스 8기')).toBeInTheDocument();
});

it('목록 카드에는 인터뷰이가 아니라 에디터를 보여준다', async () => {
  renderRoute('/magazines');

  const card = (await screen.findByRole('link', { name: '우진의 레벨 3 회고' })).closest(
    'article',
  )!;
  expect(within(card).getByText('재키')).toBeInTheDocument();
  expect(within(card).queryByText('우진')).not.toBeInTheDocument();
});

it('매거진 좋아요를 누르면 좋아요 수가 바뀐다', async () => {
  renderRoute('/magazines/2');

  const like = await screen.findByRole('button', { name: '좋아요' });
  expect(within(like).getByLabelText('좋아요 수')).toHaveTextContent('7');
  await userEvent.click(like);

  const cancel = await screen.findByRole('button', { name: '좋아요 취소' });
  expect(within(cancel).getByLabelText('좋아요 수')).toHaveTextContent('8');
});

it('매거진에 댓글을 남기면 목록과 댓글 수에 반영된다', async () => {
  renderRoute('/magazines/1');

  expect(await screen.findByText('아직 댓글이 없습니다.')).toBeInTheDocument();
  await userEvent.type(screen.getByLabelText('댓글 남기기'), '인터뷰 잘 읽었어요');
  await userEvent.click(screen.getByRole('button', { name: '댓글 작성' }));

  const list = await screen.findByRole('list', { name: '댓글 목록' });
  expect(await within(list).findByText('인터뷰 잘 읽었어요')).toBeInTheDocument();
  expect(await screen.findByLabelText('댓글 1개')).toBeInTheDocument();
});

it('매거진별 OG를 중복 없이 표시하고 목록으로 돌아오면 공통 OG로 복원한다', async () => {
  const router = renderRoute('/magazines/1');
  await screen.findByRole('heading', { level: 1, name: '우진의 레벨 3 회고' });
  const content = (property: string) =>
    document.querySelector(`meta[property="${property}"]`)?.getAttribute('content');
  expect(document.querySelectorAll('meta[property="og:title"]')).toHaveLength(1);
  expect(content('og:title')).toBe('우진의 레벨 3 회고');
  expect(content('og:description')).toBe('실제 사용자를 만나며 배운 점을 들어봤어요.');
  expect(content('og:image')).toBe('https://picsum.photos/seed/magazine-0/960/540');
  expect(content('og:type')).toBe('article');
  expect(content('og:url')).toBe('https://shout-ou.tz/magazines/1');
  expect(document.querySelector('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://shout-ou.tz/magazines/1',
  );
  expect(document.querySelector('meta[property="og:image:width"]')).toBeNull();
  await act(() => router.navigate({ to: '/magazines' }));
  await waitFor(() => expect(content('og:title')).toBe('shout-outz'));
  expect(document.querySelectorAll('meta[property="og:title"]')).toHaveLength(1);
  expect(content('og:image')).toBe('https://shout-ou.tz/og-image.jpg');
  expect(content('og:url')).toBeUndefined();
});

it('대표 이미지가 없는 매거진은 공통 OG 이미지를 사용한다', async () => {
  renderRoute('/magazines/4');
  await screen.findByRole('heading', { level: 1, name: '코드 리뷰로 배운 것들' });
  expect(document.querySelector('meta[property="og:image"]')).toHaveAttribute(
    'content',
    'https://shout-ou.tz/og-image.jpg',
  );
  expect(document.querySelector('meta[property="og:title"]')).toHaveAttribute(
    'content',
    '코드 리뷰로 배운 것들',
  );
});

it('SSG 경로는 첫 페이지 이후의 발행 매거진도 수집한다', async () => {
  const cursors: Array<string | null> = [];
  server.use(
    http.get('*/api/v1/magazines', ({ request }) => {
      const cursor = new URL(request.url).searchParams.get('cursor');
      cursors.push(cursor);
      return HttpResponse.json({
        status: 'success',
        data: [{ id: cursor ? 51 : 1 }],
        meta: { hasNext: !cursor, nextCursor: cursor ? null : 'page2', totalCount: 2 },
      });
    }),
  );
  expect(MagazineDetailRoute.options.staticData?.prerender).toBe(true);
  const params = await MagazineDetailRoute.options.staticData!.generateStaticParams!();
  expect(params).toEqual([{ magazineId: '1' }, { magazineId: '51' }]);
  expect(cursors).toEqual([null, 'page2']);
});
