import { consoleAdapter } from '@/utils/analytics/console-adapter';
import { createAnalytics } from '@/utils/analytics/create-analytics';
import { posthogAdapter } from '@/utils/analytics/posthog-adapter';
import { type AnalyticsAdapter } from '@/utils/analytics/types';

export { createAnalytics } from '@/utils/analytics/create-analytics';
export { toPathPattern } from '@/utils/analytics/path';
export type {
  AnalyticsAdapter,
  AnalyticsClient,
  AnalyticsEvent,
  AnalyticsUser,
  FeedSurface,
  NavTab,
} from '@/utils/analytics/types';

const MOBILE_MAX_WIDTH = 768;

const POSTHOG_KEY = process.env.POSTHOG_KEY;
const POSTHOG_HOST = process.env.POSTHOG_HOST ?? 'https://us.i.posthog.com';

const isBrowser = typeof window !== 'undefined';

function createAdapters(): AnalyticsAdapter[] {
  if (!isBrowser) return [];
  if (POSTHOG_KEY) return [posthogAdapter({ key: POSTHOG_KEY, host: POSTHOG_HOST })];
  if (process.env.NODE_ENV === 'development') return [consoleAdapter()];

  return [];
}

// 최종적으로 사용할 객체
export const analytics = createAnalytics({
  adapters: createAdapters(),
  getContext: () => ({
    path: typeof window === 'undefined' ? '' : window.location.pathname + window.location.search,
    viewport:
      typeof window !== 'undefined' && window.innerWidth < MOBILE_MAX_WIDTH ? 'mobile' : 'desktop',
  }),
});
