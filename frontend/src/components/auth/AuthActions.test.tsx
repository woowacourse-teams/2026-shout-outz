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

    expect(await screen.findByRole('button', { name: 'GitHub 계정으로 시작하기' })).toBeEnabled();
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
    expect(screen.getByRole('button', { name: '내 계정 메뉴' })).toBeInTheDocument();
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
    await user.click(await screen.findByRole('button', { name: '내 계정 메뉴' }));
    await user.click(screen.getByRole('menuitem', { name: '로그아웃' }));

    expect(await screen.findByRole('button', { name: '로그인' })).toBeInTheDocument();
  });

  it('프로필 메뉴에서 마이페이지로 이동하고 메뉴를 닫는다', async () => {
    const user = userEvent.setup();
    const router = renderRoute('/');
    const trigger = await screen.findByRole('button', { name: '내 계정 메뉴' });
    await within(trigger).findByText('정');
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    expect(within(trigger).queryByText('정우진')).not.toBeInTheDocument();
    expect(within(trigger).queryByText(/우아한테크코스/)).not.toBeInTheDocument();
    expect(within(trigger).queryByRole('status')).not.toBeInTheDocument();
    await user.click(trigger);
    const menu = screen.getByRole('menu');
    expect(within(menu).getByText('정우진')).toBeInTheDocument();
    expect(within(menu).getByText('@woojin')).toBeInTheDocument();
    expect(await within(menu).findByText('8기 백엔드 크루')).toBeInTheDocument();
    await user.click(within(menu).getByRole('menuitem', { name: '마이페이지' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/users/woojin'));
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('모바일 내 계정 서랍에서 프로필과 내 글·내 프로젝트로 가는 링크, 로그아웃을 보여 준다', async () => {
    renderRoute('/');
    const trigger = await screen.findByRole('button', { name: '내 계정 열기' });
    // vaul 서랍은 jsdom에서 포인터 이벤트가 막혀 키보드로 연다.
    trigger.focus();
    await userEvent.keyboard('{Enter}');

    const drawer = await screen.findByRole('dialog', { name: '내 계정' });
    expect(await within(drawer).findByText('정우진')).toBeInTheDocument();
    expect(within(drawer).getByText('@woojin')).toBeInTheDocument();
    expect(within(drawer).getByRole('link', { name: '내 프로필' })).toHaveAttribute(
      'href',
      '/users/woojin',
    );
    expect(within(drawer).getByRole('link', { name: '내가 쓴 질문 · 답변' })).toHaveAttribute(
      'href',
      '/users/woojin?tab=feeds',
    );
    expect(within(drawer).getByRole('link', { name: '내 프로젝트' })).toHaveAttribute(
      'href',
      '/users/woojin?tab=projects',
    );
    expect(within(drawer).getByRole('button', { name: '로그아웃' })).toBeInTheDocument();
  });

  it.each(['로딩', '실패'])(
    '요약 조회 %s 중에도 로그아웃은 가능하고 마이페이지는 표시하지 않는다',
    async (state) => {
      server.use(
        http.get('/api/v1/users/me/summary', async () => {
          if (state === '로딩') await delay('infinite');
          return new HttpResponse(null, { status: 500 });
        }),
      );
      const user = userEvent.setup();
      renderRoute('/');
      await user.click(await screen.findByRole('button', { name: '내 계정 메뉴' }));
      if (state === '로딩') {
        expect(
          screen.getByRole('status', { name: '프로필 정보를 불러오는 중' }),
        ).toBeInTheDocument();
      }
      expect(screen.getByRole('menuitem', { name: '로그아웃' })).toBeEnabled();
      expect(screen.queryByRole('menuitem', { name: '마이페이지' })).not.toBeInTheDocument();
    },
  );

  it('프로필 메뉴는 키보드로 이동하고 Escape와 바깥 클릭으로 닫는다', async () => {
    const user = userEvent.setup();
    renderRoute('/');
    const trigger = await screen.findByRole('button', { name: '내 계정 메뉴' });
    await within(trigger).findByText('정');
    await user.click(trigger);
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('menuitem', { name: '마이페이지' })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('menuitem', { name: '로그아웃' })).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(trigger).toHaveFocus();
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    await user.click(trigger);
    await user.click(document.body);
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });
});
