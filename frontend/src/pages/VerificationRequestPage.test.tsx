/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { renderRoute, server } from '@/test/renderRoute';

test('신청 이력이 없는 로그인 사용자가 우아한테크코스 소속 인증을 신청한다', async () => {
  const user = userEvent.setup();
  let requestBody: unknown;
  server.use(
    http.get('/api/v1/users/me/verification-request', () =>
      HttpResponse.json({ status: 'success', data: null }),
    ),
    http.post('/api/v1/users/me/verification-requests', async ({ request }) => {
      requestBody = await request.json();
      return HttpResponse.json(
        {
          status: 'success',
          data: {
            requestId: 2,
            ...(requestBody as object),
            status: 'PENDING',
            requestedAt: '2026-09-19T00:00:00Z',
            decidedAt: null,
            reason: null,
          },
        },
        { status: 201 },
      );
    }),
  );

  renderRoute('/mypage/verification');
  await user.type(await screen.findByRole('textbox', { name: '우테코 닉네임' }), '라이');
  await user.click(screen.getByRole('combobox', { name: '기수' }));
  await user.click(screen.getByRole('option', { name: '8기 (2026)' }));
  await user.click(screen.getByRole('combobox', { name: '트랙' }));
  await user.click(screen.getByRole('option', { name: '프론트엔드' }));
  expect(
    screen.getByText('우아한테크코스에서 사용하는 실제 닉네임을 입력해 주세요.'),
  ).toBeInTheDocument();
  expect(screen.getByRole('combobox', { name: '기수' })).toHaveAccessibleDescription(
    /기수를 정확히 선택/,
  );
  expect(screen.getByRole('combobox', { name: '트랙' })).toHaveAccessibleDescription(/본인의 파트/);
  expect(screen.getByText('닉네임과 파트·기수 정보를 수정할 수 없습니다.')).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: '우아한테크코스 소속 인증 신청하기' }));

  expect(requestBody).toEqual({
    userType: 'WOOWACOURSE_CREW',
    nickname: '라이',
    cohort: 8,
    track: 'FRONTEND',
  });
  expect(
    await screen.findByText('우아한테크코스 소속 인증 신청을 검토하고 있어요.'),
  ).toBeInTheDocument();
});
