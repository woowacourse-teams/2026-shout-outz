/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';

import { renderRoute, server } from '@/test/renderRoute';

describe('/users 진입', () => {
  it('내 handle의 프로필로 보낸다', async () => {
    const router = renderRoute('/users');

    await waitFor(() => expect(router.state.location.pathname).toBe('/users/woojin'));
    expect(await screen.findByRole('heading', { name: '정우진' })).toBeInTheDocument();
  });

  it('로그인하지 않았으면 안내를 보여준다', async () => {
    server.use(http.get('/api/v1/users/me/summary', () => new HttpResponse(null, { status: 401 })));

    renderRoute('/users');

    expect(await screen.findByText('로그인이 필요합니다.')).toBeInTheDocument();
  });
});
