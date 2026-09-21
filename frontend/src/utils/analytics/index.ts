import { consoleAdapter } from '@/utils/analytics/console-adapter';
import { createAnalytics } from '@/utils/analytics/create-analytics';
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

// TODO 수집 도구가 정해지면 여기에 어댑터를 추가
const adapters: AnalyticsAdapter[] =
  process.env.NODE_ENV === 'development' ? [consoleAdapter()] : [];

// 최종적으로 사용할 객체
export const analytics = createAnalytics({
  adapters,
  getContext: () => ({
    path: typeof window === 'undefined' ? '' : window.location.pathname + window.location.search,
    viewport:
      typeof window !== 'undefined' && window.innerWidth < MOBILE_MAX_WIDTH ? 'mobile' : 'desktop',
  }),
});
