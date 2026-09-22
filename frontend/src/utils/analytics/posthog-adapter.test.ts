import { type PostHog } from 'posthog-js';
import { posthogAdapter, type PostHogClient } from '@/utils/analytics/posthog-adapter';

// SDK 타입을 그대로 만족시키되, 호출만 기록한다. init과 capture는 반환값이 있어 형태만 맞춘다.
function fakePostHog() {
  const calls: { method: string; args: unknown[] }[] = [];
  const client: PostHogClient = {
    init: (...args) => {
      calls.push({ method: 'init', args });
      return client as PostHog;
    },
    capture: (...args) => {
      calls.push({ method: 'capture', args });
      return undefined;
    },
    identify: (...args) => {
      calls.push({ method: 'identify', args });
    },
    reset: (...args) => {
      calls.push({ method: 'reset', args });
    },
  };

  return { client, calls };
}

const CONTEXT = { path: '/', isLoggedIn: false, viewport: 'desktop' } as const;

const load = (client: PostHogClient) => () => Promise.resolve(client);

describe('posthogAdapter', () => {
  it('SDK가 스스로 수집하지 않도록 끄고 시작한다', async () => {
    const { client, calls } = fakePostHog();
    posthogAdapter({ key: 'phc_test', host: 'https://ph.test', load: load(client) });

    await Promise.resolve();

    expect(calls[0]?.method).toBe('init');
    expect(calls[0]?.args[0]).toBe('phc_test');
    expect(calls[0]?.args[1]).toMatchObject({
      api_host: 'https://ph.test',
      capture_pageview: false,
      autocapture: false,
    });
  });

  it('페이지뷰는 패턴 경로를 $current_url로 보낸다', async () => {
    const { client, calls } = fakePostHog();
    const adapter = posthogAdapter({ key: 'phc_test', host: 'h', load: load(client) });

    await Promise.resolve();
    adapter.pageView('/feeds/:feedId', CONTEXT);

    expect(calls.at(-1)).toEqual({
      method: 'capture',
      args: ['$pageview', { $current_url: '/feeds/:feedId', ...CONTEXT }],
    });
  });

  it('이벤트는 이름과 파라미터로 나눠 보낸다', async () => {
    const { client, calls } = fakePostHog();
    const adapter = posthogAdapter({ key: 'phc_test', host: 'h', load: load(client) });

    await Promise.resolve();
    adapter.track({ name: 'profile_tab_changed', tab: 'feeds' }, CONTEXT);

    expect(calls.at(-1)).toEqual({
      method: 'capture',
      args: ['profile_tab_changed', { tab: 'feeds', ...CONTEXT }],
    });
  });

  describe('사용자 식별', () => {
    it('식별자는 문자열로, handle은 속성으로 넘긴다', async () => {
      const { client, calls } = fakePostHog();
      const adapter = posthogAdapter({ key: 'phc_test', host: 'h', load: load(client) });

      await Promise.resolve();
      adapter.identify({ userId: 10, handle: 'woojin' });

      expect(calls.at(-1)).toEqual({ method: 'identify', args: ['10', { handle: 'woojin' }] });
    });

    it('로그아웃은 reset으로 옮긴다', async () => {
      const { client, calls } = fakePostHog();
      const adapter = posthogAdapter({ key: 'phc_test', host: 'h', load: load(client) });

      await Promise.resolve();
      adapter.identify(null);

      expect(calls.at(-1)).toEqual({ method: 'reset', args: [] });
    });
  });

  it('SDK가 도착하기 전에 생긴 이벤트도 순서대로 보낸다', async () => {
    const { client, calls } = fakePostHog();
    const adapter = posthogAdapter({ key: 'phc_test', host: 'h', load: load(client) });

    adapter.pageView('/', CONTEXT);
    adapter.track({ name: 'hero_banner_clicked' }, CONTEXT);
    expect(calls).toHaveLength(0);

    await Promise.resolve();
    await Promise.resolve();

    expect(calls.map(({ method, args }) => [method, args[0]])).toEqual([
      ['init', 'phc_test'],
      ['capture', '$pageview'],
      ['capture', 'hero_banner_clicked'],
    ]);
  });

  it('SDK를 불러오지 못해도 화면을 멈추지 않는다', async () => {
    const consoleWarn = jest.spyOn(console, 'warn').mockImplementation(() => {});
    try {
      const adapter = posthogAdapter({
        key: 'phc_test',
        host: 'h',
        load: () => Promise.reject(new Error('차단됨')),
      });

      await Promise.resolve();

      expect(() => adapter.track({ name: 'hero_banner_clicked' }, CONTEXT)).not.toThrow();
    } finally {
      consoleWarn.mockRestore();
    }
  });
});
