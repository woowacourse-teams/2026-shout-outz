import { type AnalyticsClient } from '@/utils/analytics/types';

export interface PageViewRouter {
  subscribe(
    event: 'onResolved',
    listener: (payload: { toLocation: { pathname: string; searchStr: string } }) => void,
  ): () => void;
}

export function connectRouterPageViews(
  router: PageViewRouter,
  client: AnalyticsClient,
  initialPath: string,
): () => void {
  client.pageView(initialPath);

  return router.subscribe('onResolved', ({ toLocation }) => {
    client.pageView(toLocation.pathname + toLocation.searchStr);
  });
}
