/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { adminMagazineHandlers } from '@/mocks/magazines';
import { renderRoute, server } from '@/test/renderRoute';

beforeEach(() =>
  server.use(
    http.get('/api/v1/auth/session', () =>
      HttpResponse.json({
        status: 'success',
        data: { status: 'AUTHENTICATED', userId: 1, role: 'ADMIN', csrfToken: 'token' },
      }),
    ),
    ...adminMagazineHandlers,
  ),
);

const rowOf = async (title: string) =>
  (await screen.findByText(title)).closest('li') as HTMLElement;

test('초안을 발행하고, 발행된 매거진은 발행을 취소한다', async () => {
  const user = userEvent.setup();
  const calls: string[] = [];
  server.use(
    http.post('/api/v1/admin/magazines/:magazineId/:action', ({ params }) => {
      calls.push(`${params.action}:${params.magazineId}`);
      return HttpResponse.json({ status: 'success', data: { id: Number(params.magazineId) } });
    }),
  );
  renderRoute('/admin?tab=magazines');

  const draft = await rowOf('코드 리뷰로 배운 것들');
  expect(within(draft).getByText('초안')).toBeInTheDocument();
  await user.click(within(draft).getByRole('button', { name: '발행' }));

  const published = await rowOf('우진의 레벨 3 회고');
  await user.click(within(published).getByRole('button', { name: '발행 취소' }));

  await waitFor(() => expect(calls).toEqual(['publish:4', 'unpublish:1']));
});

test('삭제는 한 번 더 확인한 뒤 요청한다', async () => {
  const user = userEvent.setup();
  let deletedId: string | undefined;
  server.use(
    http.delete('/api/v1/admin/magazines/:magazineId', ({ params }) => {
      deletedId = String(params.magazineId);
      return new HttpResponse(null, { status: 204 });
    }),
  );
  renderRoute('/admin?tab=magazines');

  const row = await rowOf('프론트엔드 크루의 하루');
  await user.click(within(row).getByRole('button', { name: '삭제' }));
  expect(deletedId).toBeUndefined();
  await user.click(within(row).getByRole('button', { name: '삭제 확정' }));

  await waitFor(() => expect(deletedId).toBe('2'));
});

test('인터뷰이를 검색해 고르고 초안을 등록한다', async () => {
  const user = userEvent.setup();
  let saved: unknown;
  server.use(
    http.post('/api/v1/admin/magazines', async ({ request }) => {
      saved = await request.json();
      return HttpResponse.json({ status: 'success', data: { id: 99 } }, { status: 201 });
    }),
  );
  renderRoute('/admin?tab=magazines');

  await user.type(await screen.findByLabelText('인터뷰이 *'), '우진');
  await user.click(await screen.findByRole('button', { name: /정우진/ }));
  await user.type(screen.getByLabelText('제목 *'), '새 매거진');
  await user.type(screen.getByLabelText('요약 (발행에 필요)'), '요약');
  await user.click(screen.getByRole('button', { name: '초안 등록' }));

  await waitFor(() =>
    expect(saved).toEqual({
      featuredUserId: expect.any(Number),
      title: '새 매거진',
      summary: '요약',
      thumbnailMediaId: null,
      contentMd: null,
    }),
  );
});

test('수정하면 원본 본문과 이미지 참조를 그대로 전체 교체로 보낸다', async () => {
  const user = userEvent.setup();
  let saved: Record<string, unknown> | undefined;
  server.use(
    http.put('/api/v1/admin/magazines/:magazineId', async ({ request }) => {
      saved = (await request.json()) as Record<string, unknown>;
      return HttpResponse.json({ status: 'success', data: { id: 1 } });
    }),
  );
  renderRoute('/admin?tab=magazines');

  const row = await rowOf('우진의 레벨 3 회고');
  await user.click(within(row).getByRole('button', { name: '수정' }));
  expect(await screen.findByRole('heading', { name: '매거진 수정 #1' })).toBeInTheDocument();
  // 상세 응답에는 대상 ID만 있어, 목록에서 본 이름을 그대로 보여준다.
  expect(screen.getByText('@crew1')).toBeInTheDocument();

  const title = screen.getByLabelText('제목 *');
  await user.clear(title);
  await user.type(title, '고친 제목');
  await user.click(screen.getByRole('button', { name: '수정 저장' }));

  await waitFor(() =>
    expect(saved).toMatchObject({
      featuredUserId: 101,
      title: '고친 제목',
      thumbnailMediaId: 501,
      contentMd: expect.stringContaining('## 자기소개 부탁드려요'),
    }),
  );
});
