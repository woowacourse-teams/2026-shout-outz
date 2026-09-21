/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderRoute, server } from '@/test/renderRoute';

describe('SignupPage', () => {
  it('가입에 필요한 정보를 전송하고 홈으로 이동한다', async () => {
    const user = userEvent.setup();
    let status = 'SIGNUP_REQUIRED';
    let requestBody: unknown;

    server.use(
      http.get('/api/v1/auth/session', () =>
        HttpResponse.json({
          status: 'success',
          data: {
            status,
            userId: status === 'AUTHENTICATED' ? 1 : null,
            role: null,
            csrfToken: 'token',
          },
        }),
      ),
      http.post('/api/v1/auth/signup', async ({ request }) => {
        requestBody = await request.json();
        status = 'AUTHENTICATED';
        return HttpResponse.json({ status: 'success', data: { userId: 1 } }, { status: 201 });
      }),
    );

    const router = renderRoute('/signup');
    await user.type(await screen.findByRole('textbox', { name: '아이디' }), 'shoutoutz_user');
    await user.type(screen.getByRole('textbox', { name: '표시 이름' }), '샤라웃');
    await user.click(screen.getByRole('button', { name: '가입하기' }));

    await waitFor(() =>
      expect(requestBody).toEqual({ handle: 'shoutoutz_user', displayName: '샤라웃' }),
    );
    await waitFor(() => expect(router.state.location.pathname).toBe('/'));
  });

  it('잘못된 아이디는 요청하지 않고 입력 오류를 보여준다', async () => {
    const user = userEvent.setup();
    let requested = false;

    server.use(
      http.get('/api/v1/auth/session', () =>
        HttpResponse.json({
          status: 'success',
          data: { status: 'SIGNUP_REQUIRED', userId: null, role: null, csrfToken: 'token' },
        }),
      ),
      http.post('/api/v1/auth/signup', () => {
        requested = true;
        return HttpResponse.json({ status: 'success', data: { userId: 1 } }, { status: 201 });
      }),
    );

    renderRoute('/signup');
    await user.type(await screen.findByRole('textbox', { name: '아이디' }), '!');
    await user.type(screen.getByRole('textbox', { name: '표시 이름' }), '샤라웃');
    await user.click(screen.getByRole('button', { name: '가입하기' }));

    expect(
      await screen.findByText('2~30자의 영문, 숫자, 밑줄, 하이픈으로 입력해 주세요.'),
    ).toBeInTheDocument();
    expect(requested).toBe(false);
  });
});
