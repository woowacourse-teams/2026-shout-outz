/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { render, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';

import { AnalyticsIdentifier } from '@/components/AnalyticsIdentifier';
import { server } from '@/test/renderRoute';
import { analytics } from '@/utils/analytics';

const renderIdentifier = () => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });

  render(
    <QueryClientProvider client={queryClient}>
      <AnalyticsIdentifier />
    </QueryClientProvider>,
  );
};

const loggedIn = () => {
  server.use(
    http.get('/api/v1/auth/session', () =>
      HttpResponse.json({
        status: 'success',
        data: { status: 'AUTHENTICATED', userId: 10, csrfToken: 'token', role: 'USER' },
      }),
    ),
    http.get('/api/v1/users/me/summary', () =>
      HttpResponse.json({
        status: 'success',
        data: { userId: 10, handle: 'woojin', displayName: '정우진', avatarImageId: null },
      }),
    ),
  );
};

describe('AnalyticsIdentifier', () => {
  let identify: jest.SpiedFunction<typeof analytics.identify>;

  beforeEach(() => {
    identify = jest.spyOn(analytics, 'identify').mockImplementation(() => {});
  });

  afterEach(() => {
    identify.mockRestore();
  });

  it('로그인한 사람의 식별자를 알린다', async () => {
    loggedIn();
    renderIdentifier();

    await waitFor(() => expect(identify).toHaveBeenCalledWith({ userId: 10, handle: 'woojin' }));
  });

  it('로그인하지 않았으면 비워서 알린다', async () => {
    renderIdentifier();

    await waitFor(() => expect(identify).toHaveBeenCalledWith(null));
  });

  it('화면을 그리지 않는다', () => {
    const { container } = render(
      <QueryClientProvider client={new QueryClient()}>
        <AnalyticsIdentifier />
      </QueryClientProvider>,
    );

    expect(container).toBeEmptyDOMElement();
  });
});
