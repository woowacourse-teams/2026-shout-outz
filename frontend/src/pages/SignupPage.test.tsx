/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor, within } from '@testing-library/react';
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
    await user.type(await screen.findByRole('textbox', { name: '사용자 아이디' }), 'woowa_test');
    expect(screen.getByText('https://shout-ou.tz/users/@woowa_test')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: '사용자 아이디' })).toHaveAccessibleDescription(
      /GitHub 아이디와 달라도 괜찮아요/,
    );
    await user.type(screen.getByRole('textbox', { name: '닉네임' }), '샤라웃');
    await user.type(screen.getByRole('textbox', { name: '한 줄 소개 (선택)' }), '안녕하세요');
    await user.type(
      screen.getByRole('textbox', { name: 'GitHub 주소 (선택)' }),
      'https://github.com/woowa',
    );
    await user.click(screen.getByRole('button', { name: '아니요' }));
    await user.click(screen.getByRole('button', { name: '가입하기' }));

    // 비워 둔 블로그 주소는 null로 보낸다.
    await waitFor(() =>
      expect(requestBody).toEqual({
        handle: '@woowa_test',
        displayName: '샤라웃',
        bio: '안녕하세요',
        githubProfileUrl: 'https://github.com/woowa',
        blogUrl: null,
      }),
    );
    await waitFor(() => expect(router.state.location.pathname).toBe('/'));
  });

  it('잘못된 handle은 요청하지 않고 입력 오류를 보여준다', async () => {
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
    await user.type(await screen.findByRole('textbox', { name: '사용자 아이디' }), 'a');
    await user.type(screen.getByRole('textbox', { name: '닉네임' }), '샤라웃');
    await user.click(screen.getByRole('button', { name: '가입하기' }));

    expect(
      await screen.findByText('2~30자의 영문, 숫자, 밑줄, 하이픈으로 입력해 주세요.'),
    ).toBeInTheDocument();
    expect(requested).toBe(false);
  });

  describe('구성원 가입', () => {
    /** 가입 전후로 세션과 CSRF 토큰이 바뀌는 서버를 흉내 내고, 요청 순서를 남긴다. */
    const mockSignupServer = ({ verificationFails = false } = {}) => {
      let status = 'SIGNUP_REQUIRED';
      const calls: string[] = [];
      const recorded: { verificationBody?: unknown; verificationCsrf?: string | null } = {};

      server.use(
        http.get('/api/v1/auth/session', () => {
          calls.push(`session:${status}`);
          return HttpResponse.json({
            status: 'success',
            data: {
              status,
              userId: status === 'AUTHENTICATED' ? 1 : null,
              role: null,
              csrfToken: status === 'AUTHENTICATED' ? 'token-after-signup' : 'token',
            },
          });
        }),
        http.post('/api/v1/auth/signup', () => {
          calls.push('signup');
          status = 'AUTHENTICATED';
          return HttpResponse.json({ status: 'success', data: { userId: 1 } }, { status: 201 });
        }),
        http.post('/api/v1/users/me/verification-requests', async ({ request }) => {
          calls.push('verification');
          recorded.verificationBody = await request.json();
          recorded.verificationCsrf = request.headers.get('X-CSRF-TOKEN');
          if (verificationFails) return new HttpResponse(null, { status: 500 });
          return HttpResponse.json(
            {
              status: 'success',
              data: {
                requestId: 2,
                ...(recorded.verificationBody as object),
                status: 'PENDING',
                requestedAt: '2026-10-08T00:00:00Z',
              },
            },
            { status: 201 },
          );
        }),
      );

      return { calls, recorded };
    };

    const fillCrewForm = async (user: ReturnType<typeof userEvent.setup>) => {
      await user.type(await screen.findByRole('textbox', { name: '사용자 아이디' }), 'woowa_test');
      await user.type(screen.getByRole('textbox', { name: '닉네임' }), '라이');
      await user.click(screen.getByRole('button', { name: '크루예요' }));
      await user.click(await screen.findByRole('combobox', { name: '기수' }));
      await user.click(screen.getByRole('option', { name: '8기 (2026)' }));
      await user.click(screen.getByRole('combobox', { name: '트랙' }));
      await user.click(screen.getByRole('option', { name: '프론트엔드' }));
      await user.click(screen.getByRole('button', { name: '가입하고 크루 인증 신청하기' }));
    };

    it('가입이 끝난 뒤 크루 인증을 이어서 신청하고 승인 대기 중임을 알린다', async () => {
      const user = userEvent.setup();
      const { calls, recorded } = mockSignupServer();

      const router = renderRoute('/signup');
      await fillCrewForm(user);

      expect(
        await screen.findByText('크루 인증 신청이 접수됐고, 지금은 승인 대기 중이에요.'),
      ).toBeInTheDocument();
      // 가입 응답을 받고 새 세션의 CSRF 토큰으로 인증을 신청한다.
      expect(calls.slice(calls.indexOf('signup'))).toEqual([
        'signup',
        'session:AUTHENTICATED',
        'verification',
      ]);
      expect(recorded.verificationCsrf).toBe('token-after-signup');
      expect(recorded.verificationBody).toEqual({
        userType: 'WOOWACOURSE_CREW',
        nickname: '라이',
        cohort: 8,
        track: 'FRONTEND',
      });

      await user.click(screen.getByRole('button', { name: '홈으로 가기' }));
      await waitFor(() => expect(router.state.location.pathname).toBe('/'));
    });

    it('인증 신청이 실패하면 가입은 됐다고 알리고 다시 신청할 곳을 안내한다', async () => {
      const user = userEvent.setup();
      mockSignupServer({ verificationFails: true });

      renderRoute('/signup');
      await fillCrewForm(user);

      expect(await screen.findByRole('alert')).toHaveTextContent(
        '크루 인증 신청은 접수되지 않았어요.',
      );
      expect(screen.getByRole('heading', { name: '가입이 완료됐어요.' })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: '크루 인증 다시 신청하기' })).toHaveAttribute(
        'href',
        '/mypage/verification',
      );
    });

    it('코치는 기수·트랙 없이 코치 인증을 신청한다', async () => {
      const user = userEvent.setup();
      const { recorded } = mockSignupServer();

      renderRoute('/signup');
      await user.type(await screen.findByRole('textbox', { name: '사용자 아이디' }), 'coach_test');
      await user.type(screen.getByRole('textbox', { name: '닉네임' }), '브라운');
      await user.click(screen.getByRole('button', { name: '코치예요' }));

      expect(screen.queryByRole('combobox', { name: '기수' })).not.toBeInTheDocument();
      expect(screen.getByText('닉네임을 수정할 수 없습니다.')).toBeInTheDocument();
      await user.click(screen.getByRole('button', { name: '가입하고 코치 인증 신청하기' }));

      expect(
        await screen.findByText('코치 인증 신청이 접수됐고, 지금은 승인 대기 중이에요.'),
      ).toBeInTheDocument();
      expect(recorded.verificationBody).toEqual({
        userType: 'WOOWACOURSE_COACH',
        nickname: '브라운',
        cohort: null,
        track: null,
      });
    });

    it('크루여도 인증 없이 가입만 할 수 있다', async () => {
      const user = userEvent.setup();
      const { calls } = mockSignupServer();

      const router = renderRoute('/signup');
      await user.type(await screen.findByRole('textbox', { name: '사용자 아이디' }), 'woowa_test');
      await user.type(screen.getByRole('textbox', { name: '닉네임' }), '라이');
      await user.click(screen.getByRole('button', { name: '크루예요' }));
      // 기수·트랙을 고르지 않아도 가입만 하는 데는 막지 않는다.
      await user.click(screen.getByRole('button', { name: '인증 없이 가입만 하기' }));

      await waitFor(() => expect(router.state.location.pathname).toBe('/'));
      expect(calls).toContain('signup');
      expect(calls).not.toContain('verification');
    });

    it('구성원 여부나 기수·트랙을 고르지 않으면 가입하지 않는다', async () => {
      const user = userEvent.setup();
      const { calls } = mockSignupServer();

      renderRoute('/signup');
      await user.type(await screen.findByRole('textbox', { name: '사용자 아이디' }), 'woowa_test');
      await user.type(screen.getByRole('textbox', { name: '닉네임' }), '라이');
      await user.click(screen.getByRole('button', { name: '가입하기' }));

      expect(await screen.findByText('우테코 크루나 코치인지 선택해 주세요.')).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: '크루예요' }));
      await user.click(screen.getByRole('button', { name: '가입하고 크루 인증 신청하기' }));

      expect(await screen.findByText('기수를 선택해 주세요.')).toBeInTheDocument();
      expect(screen.getByText('트랙을 선택해 주세요.')).toBeInTheDocument();
      expect(calls).not.toContain('signup');
    });
  });

  it('아이디 규칙을 입력하는 대로 확인해 보여준다', async () => {
    const user = userEvent.setup();
    server.use(
      http.get('/api/v1/auth/session', () =>
        HttpResponse.json({
          status: 'success',
          data: { status: 'SIGNUP_REQUIRED', userId: null, role: null, csrfToken: 'token' },
        }),
      ),
    );

    renderRoute('/signup');
    const handle = await screen.findByRole('textbox', { name: '사용자 아이디' });
    const rules = () => within(screen.getByRole('list', { name: '아이디 규칙' }));

    expect(rules().getByText(/2~30자/)).not.toHaveTextContent(/충족/);

    await user.type(handle, 'a');
    expect(rules().getByText(/영문·숫자/)).toHaveTextContent(
      '영문·숫자·밑줄(_)·하이픈(-)만 사용 충족',
    );
    expect(rules().getByText(/2~30자/)).toHaveTextContent('2~30자 미충족');

    await user.type(handle, 'b!');
    expect(rules().getByText(/영문·숫자/)).toHaveTextContent('미충족');
    expect(rules().getByText(/2~30자/)).toHaveTextContent('2~30자 충족');
  });

  describe('아이디 중복 확인', () => {
    const mockSignupRequired = () => {
      const calls: string[] = [];
      server.use(
        http.get('/api/v1/auth/session', () =>
          HttpResponse.json({
            status: 'success',
            data: { status: 'SIGNUP_REQUIRED', userId: null, role: null, csrfToken: 'token' },
          }),
        ),
        http.get('/api/v1/auth/signup/handle-availability', ({ request }) => {
          const handle = new URL(request.url).searchParams.get('handle');
          calls.push(`availability:${handle}`);
          return HttpResponse.json({ status: 'success', data: { available: handle !== '@taken' } });
        }),
        http.post('/api/v1/auth/signup', () => {
          calls.push('signup');
          return HttpResponse.json(
            {
              status: 'error',
              code: 'HANDLE_ALREADY_EXISTS',
              message: '이미 사용 중인 handle입니다.',
            },
            { status: 409 },
          );
        }),
      );
      return calls;
    };

    it('이미 쓰는 아이디면 알리고 가입하지 않는다', async () => {
      const user = userEvent.setup();
      const calls = mockSignupRequired();

      renderRoute('/signup');
      await user.type(await screen.findByRole('textbox', { name: '사용자 아이디' }), 'taken');
      await user.type(screen.getByRole('textbox', { name: '닉네임' }), '샤라웃');
      await user.click(screen.getByRole('button', { name: '아니요' }));

      expect(await screen.findByText('이미 사용 중인 아이디예요.')).toBeInTheDocument();
      await user.click(screen.getByRole('button', { name: '가입하기' }));

      expect(screen.getAllByText('이미 사용 중인 아이디예요.').length).toBeGreaterThan(0);
      expect(calls).toEqual(['availability:@taken']);
    });

    it('쓸 수 있는 아이디면 @를 붙여 확인하고 알린다', async () => {
      const user = userEvent.setup();
      const calls = mockSignupRequired();

      renderRoute('/signup');
      await user.type(await screen.findByRole('textbox', { name: '사용자 아이디' }), 'woowa_test');

      expect(await screen.findByText('사용할 수 있는 아이디예요.')).toBeInTheDocument();
      // 입력이 멈춘 뒤 한 번만 묻는다.
      expect(calls).toEqual(['availability:@woowa_test']);
    });

    it('가입 중에 다른 사람이 먼저 가져가면 아이디 칸에 알린다', async () => {
      const user = userEvent.setup();
      mockSignupRequired();

      renderRoute('/signup');
      await user.type(await screen.findByRole('textbox', { name: '사용자 아이디' }), 'woowa_test');
      await user.type(screen.getByRole('textbox', { name: '닉네임' }), '샤라웃');
      await user.click(screen.getByRole('button', { name: '아니요' }));
      await screen.findByText('사용할 수 있는 아이디예요.');
      await user.click(screen.getByRole('button', { name: '가입하기' }));

      expect(await screen.findByText('이미 사용 중인 아이디예요.')).toBeInTheDocument();
      expect(screen.queryByText('이미 사용 중인 handle입니다.')).not.toBeInTheDocument();
    });
  });

  it('주소 형식이 맞지 않으면 가입하지 않고 알린다', async () => {
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
    await user.type(await screen.findByRole('textbox', { name: '사용자 아이디' }), 'woowa_test');
    await user.type(screen.getByRole('textbox', { name: '닉네임' }), '샤라웃');
    await user.type(
      screen.getByRole('textbox', { name: 'GitHub 주소 (선택)' }),
      'github.com/woowa',
    );
    await user.type(screen.getByRole('textbox', { name: '블로그 주소 (선택)' }), 'woowa.log');
    await user.click(screen.getByRole('button', { name: '가입하기' }));

    expect(
      await screen.findByText('https://github.com/아이디 형식으로 입력해 주세요.'),
    ).toBeInTheDocument();
    expect(
      screen.getByText('http:// 또는 https://로 시작하는 주소를 입력해 주세요.'),
    ).toBeInTheDocument();
    expect(requested).toBe(false);
  });
});
