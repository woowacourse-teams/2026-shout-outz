import { toPathPattern } from '@/utils/analytics/path';
import {
  type AnalyticsAdapter,
  type AnalyticsClient,
  type AnalyticsContextInput,
  type AnalyticsUser,
} from '@/utils/analytics/types';

const isSameUser = (left: AnalyticsUser | null, right: AnalyticsUser | null) =>
  left?.userId === right?.userId && left?.handle === right?.handle;

interface CreateAnalyticsOptions {
  adapters: AnalyticsAdapter[];
  // 모든 이벤트에 붙일 공통 값, 최신 값을 위해 불러올 수 있는 함수 형태로 구현
  getContext: () => AnalyticsContextInput;
}

// 여러 수집 도구에 같은 이벤트를 보내는 유틸.
export function createAnalytics({ adapters, getContext }: CreateAnalyticsOptions): AnalyticsClient {
  let currentUser: AnalyticsUser | null = null;

  const readContext = () => ({ ...getContext(), isLoggedIn: currentUser !== null });

  const forEachAdapter = (run: (adapter: AnalyticsAdapter) => void) => {
    for (const adapter of adapters) {
      try {
        run(adapter);
      } catch (error) {
        console.warn(`[analytics] ${adapter.name} 어댑터에서 오류가 났습니다.`, error);
      }
    }
  };

  return {
    pageView: (path) => {
      const pattern = toPathPattern(path);
      const context = readContext();
      forEachAdapter((adapter) => adapter.pageView(pattern, context));
    },
    track: (event) => {
      const context = readContext();
      forEachAdapter((adapter) => adapter.track(event, context));
    },
    identify: (user: AnalyticsUser | null) => {
      if (isSameUser(currentUser, user)) return;

      currentUser = user;
      forEachAdapter((adapter) => adapter.identify(user));
    },
  };
}
