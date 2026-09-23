import { connectRouterPageViews, type PageViewRouter } from '@/utils/analytics/connect';
import { type AnalyticsClient } from '@/utils/analytics/types';

function fakeRouter() {
  let notify: ((payload: { toLocation: { pathname: string; searchStr: string } }) => void) | null =
    null;
  let unsubscribed = false;

  const router: PageViewRouter = {
    subscribe: (_event, listener) => {
      notify = listener;
      return () => {
        unsubscribed = true;
      };
    },
  };

  return {
    router,
    navigate: (pathname: string, searchStr = '') =>
      notify?.({ toLocation: { pathname, searchStr } }),
    get unsubscribed() {
      return unsubscribed;
    },
  };
}

function fakeClient() {
  const paths: string[] = [];
  const client: AnalyticsClient = {
    pageView: (path) => paths.push(path),
    track: () => {},
    identify: () => {},
  };

  return { client, paths };
}

describe('connectRouterPageViews', () => {
  it('화면을 옮길 때마다 보낸다', () => {
    const { router, navigate } = fakeRouter();
    const { client, paths } = fakeClient();
    connectRouterPageViews(router, client);

    navigate('/');
    navigate('/users/woojin', '?tab=feeds');
    navigate('/feeds/101');

    expect(paths).toEqual(['/', '/users/woojin?tab=feeds', '/feeds/101']);
  });

  it('연결을 끊으면 더 보내지 않는다', () => {
    const fake = fakeRouter();
    const { client } = fakeClient();

    connectRouterPageViews(fake.router, client)();

    expect(fake.unsubscribed).toBe(true);
  });
});
