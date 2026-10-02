/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor, within } from '@testing-library/react';
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

    expect(screen.queryByRole('button', { name: '로그인' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '로그아웃' })).not.toBeInTheDocument();
    expect(await screen.findByRole('button', { name: '로그인' })).toBeInTheDocument();
  });

  it('비로그인 상태에서 로그인 시트로 GitHub 링크를 안내한다', async () => {
    server.use(
      http.get('/api/v1/auth/session', () =>
        HttpResponse.json({
          status: 'success',
          data: { status: 'UNAUTHENTICATED', userId: null, role: null, csrfToken: 'token' },
        }),
      ),
    );

    const user = userEvent.setup();
    renderRoute('/');

    await user.click(await screen.findByRole('button', { name: '로그인' }));

    expect(await screen.findByRole('link', { name: 'GitHub 계정으로 시작하기' })).toHaveAttribute(
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

    expect(await screen.findByRole('button', { name: '로그인' })).toBeInTheDocument();
  });

  it('로그인한 사용자의 이름을 내 프로필로 연결한다', async () => {
    renderRoute('/');

    expect(await screen.findByRole('link', { name: '정우진' })).toHaveAttribute(
      'href',
      '/users/woojin',
    );
  });

  it('누구인지 알기 전에는 프로필 링크를 내지 않는다', async () => {
    server.use(
      http.get('/api/v1/users/me/summary', async () => {
        await delay('infinite');
        return new HttpResponse(null);
      }),
    );

    renderRoute('/');
    // 로그아웃 버튼이 떴다는 건 인증 분기까지 렌더가 끝났다는 뜻이다.
    await screen.findByRole('button', { name: '로그아웃' });

    expect(screen.queryByRole('link', { name: /프로필|정우진|로그인/ })).not.toBeInTheDocument();
  });

  it('요약 조회에 실패해도 프로필 링크를 내지 않는다', async () => {
    server.use(http.get('/api/v1/users/me/summary', () => new HttpResponse(null, { status: 500 })));

    renderRoute('/');
    await screen.findByRole('button', { name: '로그아웃' });

    await waitFor(() =>
      expect(screen.queryByRole('link', { name: /프로필|정우진|로그인/ })).not.toBeInTheDocument(),
    );
  });

  it('프로필 링크에 아바타를 함께 보여준다', async () => {
    renderRoute('/');

    // mock의 avatarUrl이 null이라 이름 첫 글자로 만든 기본 프로필이 나온다.
    const link = await screen.findByRole('link', { name: '정우진' });
    expect(within(link).getByText('정')).toBeInTheDocument();
  });

  it('아바타 옆 이름은 좁은 화면에서도 DOM에 남는다', async () => {
    renderRoute('/');

    // sr-only로 감출 뿐이라 링크의 접근성 이름은 화면 너비와 무관하게 유지된다.
    const link = await screen.findByRole('link', { name: '정우진' });
    expect(within(link).getByText('정우진')).toHaveClass('sr-only');
  });
});
