import { type PostHog } from 'posthog-js';
import { type AnalyticsAdapter } from '@/utils/analytics/types';

// posthog-js에서 실제로 쓰는 메서드만 추려낸 타입. 테스트에서 가짜 SDK를 넣기 위함.
export type PostHogClient = Pick<PostHog, 'init' | 'capture' | 'identify' | 'reset'>;

export interface PosthogAdapterOptions {
  key: string;
  host: string;
  // 기본값은 posthog-js를 지연 로드하며, 테스트에서만 변경.
  load?: () => Promise<PostHogClient>;
}

const loadPostHog = async (): Promise<PostHogClient> => (await import('posthog-js')).default;

export function posthogAdapter({
  key,
  host,
  load = loadPostHog,
}: PosthogAdapterOptions): AnalyticsAdapter {
  let client: PostHogClient | null = null;
  let failed = false;
  const pending: ((ready: PostHogClient) => void)[] = [];

  load()
    .then((loaded) => {
      loaded.init(key, {
        api_host: host,
        capture_pageview: false,
        autocapture: false,
      });
      client = loaded;
      pending.forEach((run) => run(loaded));
      pending.length = 0;
    })
    .catch((error: unknown) => {
      failed = true;
      pending.length = 0;
      console.warn('[analytics] PostHog를 불러오지 못했습니다.', error);
    });

  const run = (fn: (ready: PostHogClient) => void) => {
    if (failed) return;
    if (client) {
      fn(client);
      return;
    }
    pending.push(fn);
  };

  return {
    name: 'posthog',
    pageView: (path, context) =>
      run((ready) => ready.capture('$pageview', { $current_url: path, ...context })),
    track: ({ name, ...properties }, context) =>
      run((ready) => ready.capture(name, { ...properties, ...context })),
    identify: (user) =>
      run((ready) =>
        user ? ready.identify(String(user.userId), { handle: user.handle }) : ready.reset(),
      ),
  };
}
