import { createAnalytics } from '@/utils/analytics/create-analytics';
import {
  type AnalyticsAdapter,
  type AnalyticsContext,
  type AnalyticsContextInput,
  type AnalyticsEvent,
  type AnalyticsUser,
} from '@/utils/analytics/types';

interface Recorded {
  pageViews: { path: string; context: AnalyticsContext }[];
  events: { event: AnalyticsEvent; context: AnalyticsContext }[];
  users: (AnalyticsUser | null)[];
}

const recordingAdapter = (name = 'recording') => {
  const recorded: Recorded = { pageViews: [], events: [], users: [] };
  const adapter: AnalyticsAdapter = {
    name,
    pageView: (path, context) => recorded.pageViews.push({ path, context }),
    track: (event, context) => recorded.events.push({ event, context }),
    identify: (user) => recorded.users.push(user),
  };

  return { adapter, recorded };
};

const CONTEXT: AnalyticsContextInput = { path: '/', viewport: 'desktop' };

describe('createAnalytics', () => {
  it('이벤트를 모든 어댑터에 그대로 보낸다', () => {
    const first = recordingAdapter('first');
    const second = recordingAdapter('second');
    const analytics = createAnalytics({
      adapters: [first.adapter, second.adapter],
      getContext: () => CONTEXT,
    });

    analytics.track({ name: 'hero_banner_clicked' });

    expect(first.recorded.events[0]?.event).toEqual({ name: 'hero_banner_clicked' });
    expect(second.recorded.events[0]?.event).toEqual({ name: 'hero_banner_clicked' });
  });

  it('공통 파라미터를 붙여서 보낸다', () => {
    const { adapter, recorded } = recordingAdapter();
    const analytics = createAnalytics({
      adapters: [adapter],
      getContext: () => ({ path: '/users/:handle', viewport: 'mobile' }),
    });

    analytics.identify({ userId: 10, handle: 'woojin' });
    analytics.track({ name: 'profile_tab_changed', tab: 'feeds' });

    expect(recorded.events[0]?.context).toEqual({
      path: '/users/:handle',
      isLoggedIn: true,
      viewport: 'mobile',
    });
  });

  it('어댑터에는 원본 경로가 아니라 패턴으로 바꾼 경로를 넘긴다', () => {
    const { adapter, recorded } = recordingAdapter();
    const analytics = createAnalytics({ adapters: [adapter], getContext: () => CONTEXT });

    analytics.pageView('/feeds/101');

    expect(recorded.pageViews[0]?.path).toBe('/feeds/:feedId');
  });

  it('사용자 식별자를 어댑터에 넘긴다', () => {
    const { adapter, recorded } = recordingAdapter();
    const analytics = createAnalytics({ adapters: [adapter], getContext: () => CONTEXT });

    analytics.identify({ userId: 10, handle: 'woojin' });
    analytics.identify(null);

    expect(recorded.users).toEqual([{ userId: 10, handle: 'woojin' }, null]);
  });

  describe('어댑터가 실패해도 화면은 멈추지 않는다', () => {
    const brokenAdapter: AnalyticsAdapter = {
      name: 'broken',
      pageView: () => {
        throw new Error('스크립트가 차단됨');
      },
      track: () => {
        throw new Error('스크립트가 차단됨');
      },
      identify: () => {
        throw new Error('스크립트가 차단됨');
      },
    };

    it('던지지 않고 나머지 어댑터로 계속 보낸다', () => {
      const { adapter, recorded } = recordingAdapter();
      const analytics = createAnalytics({
        adapters: [brokenAdapter, adapter],
        getContext: () => CONTEXT,
      });

      expect(() => analytics.track({ name: 'hero_banner_clicked' })).not.toThrow();
      expect(() => analytics.pageView('/')).not.toThrow();
      expect(() => analytics.identify(null)).not.toThrow();
      expect(recorded.events).toHaveLength(1);
    });
  });

  describe('사용자 식별', () => {
    it('로그인 여부를 identify로 판단해 공통 파라미터에 넣는다', () => {
      const { adapter, recorded } = recordingAdapter();
      const analytics = createAnalytics({ adapters: [adapter], getContext: () => CONTEXT });

      analytics.track({ name: 'hero_banner_clicked' });
      analytics.identify({ userId: 10, handle: 'woojin' });
      analytics.track({ name: 'hero_banner_clicked' });
      analytics.identify(null);
      analytics.track({ name: 'hero_banner_clicked' });

      expect(recorded.events.map(({ context }) => context.isLoggedIn)).toEqual([
        false,
        true,
        false,
      ]);
    });

    it('같은 사용자로 다시 알리면 어댑터를 부르지 않는다', () => {
      const { adapter, recorded } = recordingAdapter();
      const analytics = createAnalytics({ adapters: [adapter], getContext: () => CONTEXT });

      analytics.identify({ userId: 10, handle: 'woojin' });
      analytics.identify({ userId: 10, handle: 'woojin' });

      expect(recorded.users).toHaveLength(1);
    });

    it('사용자가 바뀌면 다시 알린다', () => {
      const { adapter, recorded } = recordingAdapter();
      const analytics = createAnalytics({ adapters: [adapter], getContext: () => CONTEXT });

      analytics.identify({ userId: 10, handle: 'woojin' });
      analytics.identify({ userId: 11, handle: 'dohyun' });
      analytics.identify(null);
      analytics.identify(null);

      expect(recorded.users).toEqual([
        { userId: 10, handle: 'woojin' },
        { userId: 11, handle: 'dohyun' },
        null,
      ]);
    });
  });

  it('어댑터가 없으면 아무 일도 하지 않는다', () => {
    const analytics = createAnalytics({ adapters: [], getContext: () => CONTEXT });

    expect(() => analytics.track({ name: 'hero_banner_clicked' })).not.toThrow();
  });
});
