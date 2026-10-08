/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderRoute, server } from '@/test/renderRoute';

const openReport = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.click(await screen.findByRole('button', { name: '버그 제보하기' }));
  return screen.findByRole('dialog');
};

describe('BugReportButton', () => {
  it('어느 화면에서든 제보 내용과 화면 경로를 함께 보낸다', async () => {
    const user = userEvent.setup();
    let body: unknown;
    server.use(
      http.post('/api/v1/bug-reports', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(
          { status: 'success', data: { bugReportId: 7, status: 'OPEN', createdAt: '' } },
          { status: 201 },
        );
      }),
    );

    renderRoute('/news');
    const dialog = await openReport(user);
    await user.type(
      within(dialog).getByLabelText('어떤 문제가 있었나요?'),
      '  좋아요가 안 눌려요  ',
    );
    await user.click(within(dialog).getByRole('button', { name: '보내기' }));

    expect(await within(dialog).findByText('제보가 접수됐어요')).toBeInTheDocument();
    expect(body).toEqual({ content: '좋아요가 안 눌려요\n\n---\n제보한 화면: /news' });

    await user.click(within(dialog).getByRole('button', { name: '확인' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('내용이 비어 있으면 보내지 않고 알린다', async () => {
    const user = userEvent.setup();
    let requested = false;
    server.use(
      http.post('/api/v1/bug-reports', () => {
        requested = true;
        return new HttpResponse(null, { status: 500 });
      }),
    );

    renderRoute('/');
    const dialog = await openReport(user);
    await user.type(within(dialog).getByLabelText('어떤 문제가 있었나요?'), '   ');
    await user.click(within(dialog).getByRole('button', { name: '보내기' }));

    expect(within(dialog).getByText('어떤 문제가 있었는지 적어 주세요.')).toBeInTheDocument();
    expect(requested).toBe(false);
  });

  it('보내지 못하면 창 안에서 알리고 쓴 내용은 남긴다', async () => {
    const user = userEvent.setup();
    server.use(http.post('/api/v1/bug-reports', () => new HttpResponse(null, { status: 500 })));

    renderRoute('/');
    const dialog = await openReport(user);
    const textarea = within(dialog).getByLabelText('어떤 문제가 있었나요?');
    await user.type(textarea, '화면이 깨져요');
    await user.click(within(dialog).getByRole('button', { name: '보내기' }));

    expect(await within(dialog).findByRole('alert')).toHaveTextContent(
      '요청에 실패했습니다. 다시 시도해 주세요.',
    );
    expect(textarea).toHaveValue('화면이 깨져요');
  });
});
