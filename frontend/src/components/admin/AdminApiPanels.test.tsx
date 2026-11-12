/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderRoute, server } from '@/test/renderRoute';

beforeEach(() =>
  server.use(
    http.get('/api/v1/auth/session', () =>
      HttpResponse.json({
        status: 'success',
        data: { status: 'AUTHENTICATED', userId: 1, role: 'ADMIN', csrfToken: 'token' },
      }),
    ),
  ),
);

const rowOf = async (text: string) => (await screen.findByText(text)).closest('li') as HTMLElement;

describe('채용공고', () => {
  test('본문에서 추출한 초안으로 새 공고를 등록한다', async () => {
    const user = userEvent.setup();
    let saved: Record<string, unknown> | undefined;
    server.use(
      http.post('/api/v1/admin/job-postings', async ({ request }) => {
        saved = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ status: 'success', data: { jobPostingId: 2 } }, { status: 201 });
      }),
    );
    renderRoute('/admin?tab=job-postings');

    await user.click(await screen.findByRole('button', { name: '새 채용공고' }));
    await user.type(screen.getByLabelText('원문 주소 *'), 'https://example.com/jobs/1');
    await user.type(screen.getByLabelText('공고 본문 *'), '자바 백엔드 개발자를 찾습니다');
    await user.click(screen.getByRole('button', { name: '본문에서 추출' }));

    expect(await screen.findByDisplayValue('우아한형제들')).toBeInTheDocument();
    expect(screen.getByLabelText('요구사항 1 이름')).toHaveValue('Java');
    await user.click(screen.getByRole('button', { name: '등록' }));

    await waitFor(() =>
      expect(saved).toMatchObject({
        sourceUrl: 'https://example.com/jobs/1',
        companyName: '우아한형제들',
        jobRole: 'BACKEND',
        requirements: [{ name: 'Java' }, { name: '대용량 트래픽' }],
      }),
    );
  });

  test('목록에서 숨기기·실패만 재분석·삭제를 요청한다', async () => {
    const user = userEvent.setup();
    const calls: string[] = [];
    server.use(
      http.patch('/api/v1/admin/job-postings/:id/visibility', async ({ params, request }) => {
        const { hidden } = (await request.json()) as { hidden: boolean };
        calls.push(`visibility:${params.id}:${hidden}`);
        return HttpResponse.json({ status: 'success', data: {} });
      }),
      http.post('/api/v1/admin/job-postings/:id/reanalysis', async ({ params, request }) => {
        const { scope } = (await request.json()) as { scope: string };
        calls.push(`reanalysis:${params.id}:${scope}`);
        return HttpResponse.json({
          status: 'success',
          data: { jobPostingId: 1, reanalyzedRequirementCount: 1, status: 'PROCESSING' },
        });
      }),
      http.delete('/api/v1/admin/job-postings/:id', ({ params }) => {
        calls.push(`delete:${params.id}`);
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderRoute('/admin?tab=job-postings');

    const row = await rowOf('백엔드 개발자');
    await user.click(within(row).getByRole('button', { name: '숨기기' }));
    await user.click(within(row).getByRole('button', { name: '실패만 재분석' }));
    expect(await within(row).findByText('요구사항 1개를 다시 분석합니다.')).toBeInTheDocument();
    await user.click(within(row).getByRole('button', { name: '삭제' }));
    await user.click(within(row).getByRole('button', { name: '삭제 확정' }));

    await waitFor(() =>
      expect(calls).toEqual(['visibility:1:true', 'reanalysis:1:FAILED', 'delete:1']),
    );
  });

  test('수정할 때 본문을 비워 두면 content를 보내지 않는다', async () => {
    const user = userEvent.setup();
    let saved: Record<string, unknown> | undefined;
    server.use(
      http.put('/api/v1/admin/job-postings/:id', async ({ request }) => {
        saved = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ status: 'success', data: { jobPostingId: 1 } });
      }),
    );
    renderRoute('/admin?tab=job-postings');

    const row = await rowOf('백엔드 개발자');
    await user.click(within(row).getByRole('button', { name: '수정' }));
    const title = await within(row).findByLabelText('제목 *');
    await user.clear(title);
    await user.type(title, '서버 개발자');
    await user.click(within(row).getByRole('button', { name: '저장' }));

    await waitFor(() => expect(saved).toMatchObject({ title: '서버 개발자', jobRole: 'BACKEND' }));
    expect(saved).not.toHaveProperty('content');
  });

  test('상세를 열면 요구사항별 분석 상태를 보여 준다', async () => {
    const user = userEvent.setup();
    renderRoute('/admin?tab=job-postings');

    const row = await rowOf('백엔드 개발자');
    await user.click(within(row).getByRole('button', { name: '상세' }));

    expect(await within(row).findByText('NO_CANDIDATES')).toBeInTheDocument();
    expect(within(row).getByText('대용량 트래픽 처리 경험')).toBeInTheDocument();
  });
});

test('프로젝트 승인 목록에서 심사 이력을 펼쳐 본다', async () => {
  const user = userEvent.setup();
  renderRoute('/admin?tab=projects');

  const [historyButton] = await screen.findAllByRole('button', { name: /심사 이력 보기$/ });
  if (!historyButton) throw new Error('심사 이력 버튼이 없습니다.');
  await user.click(historyButton);

  expect(await screen.findByText(/없음 → 대기/)).toBeInTheDocument();
  expect(screen.getByText('· @woowa-admin')).toBeInTheDocument();
});

test('카테고리 표시 이름을 바꿔 저장하고, 새 카테고리를 추가한다', async () => {
  const user = userEvent.setup();
  const calls: unknown[] = [];
  server.use(
    http.get('/api/v1/categories', () =>
      HttpResponse.json({
        status: 'success',
        data: [
          {
            categoryId: 1,
            slug: 'backend',
            displayName: '백엔드',
            type: 'GENERAL',
            feedType: 'POST',
            displayOrder: 1,
          },
        ],
      }),
    ),
    http.put('/api/v1/categories/:id', async ({ params, request }) => {
      calls.push({ id: params.id, ...((await request.json()) as object) });
      return HttpResponse.json({ status: 'success', data: {} });
    }),
    http.post('/api/v1/categories', async ({ request }) => {
      calls.push(await request.json());
      return HttpResponse.json({ status: 'success', data: {} }, { status: 201 });
    }),
  );
  renderRoute('/admin?tab=categories');

  const name = await screen.findByLabelText('백엔드 표시 이름');
  await user.clear(name);
  await user.type(name, '서버');
  await user.click(within(name.closest('li') as HTMLElement).getByRole('button', { name: '저장' }));

  await user.type(screen.getByLabelText('slug *'), 'free');
  await user.type(screen.getByLabelText('표시 이름 *'), '자유');
  await user.click(screen.getByRole('button', { name: '추가' }));

  await waitFor(() =>
    expect(calls).toEqual([
      { id: '1', displayName: '서버', displayOrder: 1 },
      { feedType: 'POST', slug: 'free', displayName: '자유', displayOrder: 0, type: 'GENERAL' },
    ]),
  );
});
