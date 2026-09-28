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

describe('가입 시 프로필 사진', () => {
  const signupSession = () =>
    http.get('/api/v1/auth/session', () =>
      HttpResponse.json({
        status: 'success',
        data: { status: 'SIGNUP_REQUIRED', userId: null, role: null, csrfToken: 'token' },
      }),
    );

  const fillForm = async (user: ReturnType<typeof userEvent.setup>) => {
    await user.type(await screen.findByRole('textbox', { name: '아이디' }), 'zzaekkii');
    await user.type(screen.getByRole('textbox', { name: '표시 이름' }), '재키');
  };

  it('사진을 고르지 않으면 프로필 저장을 부르지 않는다', async () => {
    const user = userEvent.setup();
    let profileCalls = 0;
    server.use(
      signupSession(),
      http.post('/api/v1/auth/signup', () =>
        HttpResponse.json({ status: 'success', data: { userId: 1 } }, { status: 201 }),
      ),
      http.put('/api/v1/users/me', () => {
        profileCalls += 1;
        return HttpResponse.json({ status: 'success', data: {} });
      }),
    );

    renderRoute('/signup');
    await fillForm(user);
    await user.click(screen.getByRole('button', { name: '가입하기' }));

    await waitFor(() => expect(screen.getByRole('button', { name: '가입하기' })).toBeEnabled());
    expect(profileCalls).toBe(0);
  });

  it('사진을 고르면 가입 직후 프로필에 저장한다', async () => {
    const user = userEvent.setup();
    let body: unknown;
    server.use(
      signupSession(),
      http.post('/api/v1/auth/signup', () =>
        HttpResponse.json({ status: 'success', data: { userId: 1 } }, { status: 201 }),
      ),
      http.put('/api/v1/users/me', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: 'success', data: {} });
      }),
    );

    renderRoute('/signup');
    await fillForm(user);

    await user.upload(
      screen.getByLabelText('프로필 사진 추가'),
      new File(['x'], 'me.png', { type: 'image/png' }),
    );
    await waitFor(() =>
      expect(screen.getByRole('button', { name: '프로필 사진 추가' })).toBeEnabled(),
    );

    await user.click(screen.getByRole('button', { name: '가입하기' }));

    await waitFor(() => expect(body).toHaveProperty('avatarImageId', 12));
    expect(body).toHaveProperty('displayName', '재키');
  });

  it('고른 이름의 첫 글자를 기본 프로필로 보여준다', async () => {
    const user = userEvent.setup();
    server.use(signupSession());

    renderRoute('/signup');
    await user.type(await screen.findByRole('textbox', { name: '표시 이름' }), '재키');

    expect(screen.getByText('재')).toBeInTheDocument();
  });
});
