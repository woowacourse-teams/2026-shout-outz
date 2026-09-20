/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { delay, http, HttpResponse } from 'msw';

import { renderRoute, server } from '@/test/renderRoute';

describe('AuthActions', () => {
  it('로그인 상태를 확인하는 동안 인증 버튼을 노출하지 않는다', async () => {
    server.use(
      http.get('/api/v1/auth/session', async () => {
        await delay(100);
        return HttpResponse.json({
          status: 'success',
          data: { status: 'UNAUTHENTICATED', userId: null, role: null, csrfToken: 'token' },
        });
      }),
    );

    renderRoute('/');

    expect(screen.queryByRole('link', { name: '로그인' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '로그아웃' })).not.toBeInTheDocument();
    expect(await screen.findByRole('link', { name: '로그인' })).toBeInTheDocument();
  });

  it('비로그인 상태에서 GitHub 로그인 링크를 보여준다', async () => {
    server.use(
      http.get('/api/v1/auth/session', () =>
        HttpResponse.json({
          status: 'success',
          data: { status: 'UNAUTHENTICATED', userId: null, role: null, csrfToken: 'token' },
        }),
      ),
    );

    renderRoute('/');

    expect(await screen.findByRole('link', { name: '로그인' })).toHaveAttribute(
      'href',
      'http://localhost/oauth2/authorization/github',
    );
  });

  it('가입이 필요한 사용자를 가입 페이지로 이동시킨다', async () => {
    server.use(
      http.get('/api/v1/auth/session', () =>
        HttpResponse.json({
          status: 'success',
          data: { status: 'SIGNUP_REQUIRED', userId: null, role: null, csrfToken: 'token' },
        }),
      ),
    );

    const router = renderRoute('/');

    await waitFor(() => expect(router.state.location.pathname).toBe('/signup'));
    expect(screen.getByRole('button', { name: '로그아웃' })).toBeInTheDocument();
  });

  it('로그아웃 후 비로그인 상태로 갱신한다', async () => {
    const user = userEvent.setup();
    let authenticated = true;

    server.use(
      http.get('/api/v1/auth/session', () =>
        HttpResponse.json({
          status: 'success',
          data: {
            status: authenticated ? 'AUTHENTICATED' : 'UNAUTHENTICATED',
            userId: authenticated ? 1 : null,
            role: authenticated ? 'USER' : null,
            csrfToken: 'token',
          },
        }),
      ),
      http.post('/api/v1/auth/logout', () => {
        authenticated = false;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    renderRoute('/');
    await user.click(await screen.findByRole('button', { name: '로그아웃' }));

    expect(await screen.findByRole('link', { name: '로그인' })).toBeInTheDocument();
  });

  it('로그인한 사용자의 이름을 내 프로필로 연결한다', async () => {
    renderRoute('/');

    expect(await screen.findByRole('link', { name: '정우진' })).toHaveAttribute('href', '/users');
  });
});
