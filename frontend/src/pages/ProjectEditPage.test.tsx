/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { renderRoute, server } from '@/test/renderRoute';

it('수정 시 작성자를 맨 앞에 고정하고 추가 팀원만 전송한다', async () => {
  const user = userEvent.setup();
  let body: unknown;
  server.use(
    http.put('/api/v1/projects/1', async ({ request }) => {
      body = await request.json();
      return HttpResponse.json({
        status: 'success',
        data: { projectId: 1, approvalStatus: 'APPROVED' },
      });
    }),
  );

  renderRoute('/projects/1/edit');

  const members = await screen.findByRole('list', { name: '선택한 참여 팀원' });
  expect(
    within(members)
      .getAllByRole('listitem')
      .map((item) => item.textContent),
  ).toEqual(['정우진 (작성자)', '두리', '재키']);

  await user.click(screen.getByRole('button', { name: '참여 팀원 변경' }));
  await user.click(screen.getByRole('button', { name: '선택 초기화' }));
  expect(screen.getByText('작성자 · 항상 포함')).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: '팀원 추가하기' }));
  await user.click(screen.getByRole('button', { name: '프로젝트 수정하기' }));
  expect(screen.getByText('참여 팀원을 1명 이상 선택해 주세요.')).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: '참여 팀원 추가' }));
  await user.type(screen.getByRole('searchbox', { name: '크루 검색' }), '재키');
  const results = await screen.findByRole('list', { name: '크루 검색 결과' });
  await user.click(within(results).getByRole('checkbox', { name: /재키/ }));
  await user.click(screen.getByRole('button', { name: '1명 팀원 추가하기' }));
  await user.click(screen.getByRole('button', { name: '프로젝트 수정하기' }));

  await waitFor(() =>
    expect(body).toEqual(expect.objectContaining({ memberHandles: ['zzaekkii'] })),
  );
});
