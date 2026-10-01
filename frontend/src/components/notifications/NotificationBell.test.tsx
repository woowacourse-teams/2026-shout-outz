/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderRoute, server } from '@/test/renderRoute';
import type { NotificationItem } from '@/types/notification';

const notification = (overrides: Partial<NotificationItem> = {}): NotificationItem => ({
  notificationId: 100,
  notificationType: 'QUESTION_ACTIVITY',
  message: '새로운 답변이 달렸어요.',
  feedId: 1,
  feedTitle: 'WebSocket 동기화 개선기',
  commentId: 20,
  actor: { userId: 30, handle: 'actor', displayName: '작성자', avatarUrl: null },
  isRead: false,
  createdAt: new Date().toISOString(),
  ...overrides,
});

/** 목록과 읽지 않은 수를 고정하고, 읽음 요청을 기록한다. */
const mockNotifications = (items: NotificationItem[]) => {
  const reads: string[] = [];

  server.use(
    http.get('/api/v1/notifications', () =>
      HttpResponse.json({
        status: 'success',
        data: items,
        meta: { hasNext: false, nextCursor: null, totalCount: items.length },
      }),
    ),
    http.get('/api/v1/notifications/unread-count', () =>
      HttpResponse.json({
        status: 'success',
        data: { unreadCount: items.filter((item) => !item.isRead).length },
      }),
    ),
    http.patch('/api/v1/notifications/read-all', () => {
      reads.push('all');
      return new HttpResponse(null, { status: 204 });
    }),
    http.patch('/api/v1/notifications/:notificationId/read', ({ params }) => {
      reads.push(String(params.notificationId));
      return HttpResponse.json({
        status: 'success',
        data: { notificationId: Number(params.notificationId), isRead: true },
      });
    }),
  );

  return reads;
};

describe('NotificationBell', () => {
  it('읽지 않은 알림 수를 버튼에 알린다', async () => {
    mockNotifications([notification(), notification({ notificationId: 101, isRead: true })]);
    renderRoute('/');

    expect(
      await screen.findByRole('button', { name: '알림, 읽지 않은 알림 1개' }),
    ).toBeInTheDocument();
  });

  it('비로그인 상태에서는 알림 버튼을 그리지 않는다', async () => {
    server.use(
      http.get('/api/v1/auth/session', () =>
        HttpResponse.json({
          status: 'success',
          data: { status: 'UNAUTHENTICATED', userId: null, role: null, csrfToken: 'token' },
        }),
      ),
    );
    renderRoute('/');

    await screen.findByRole('button', { name: '로그인' });
    expect(screen.queryByRole('button', { name: /^알림/ })).not.toBeInTheDocument();
  });

  it('알림 박스를 열면 목록을 보여준다', async () => {
    mockNotifications([notification()]);
    const user = userEvent.setup();
    renderRoute('/');

    await user.click(await screen.findByRole('button', { name: /^알림/ }));

    const panel = screen.getByRole('region', { name: '알림' });
    expect(await within(panel).findByText('새로운 답변이 달렸어요.')).toBeInTheDocument();
    expect(within(panel).getByText('WebSocket 동기화 개선기')).toBeInTheDocument();
    expect(within(panel).getByText('방금 전')).toBeInTheDocument();
  });

  it('알림이 없으면 빈 상태 문구를 보여준다', async () => {
    mockNotifications([]);
    const user = userEvent.setup();
    renderRoute('/');

    await user.click(await screen.findByRole('button', { name: '알림' }));

    expect(await screen.findByText('새로운 활동이 없어요.')).toBeInTheDocument();
  });

  it('목록을 불러오지 못하면 오류를 알린다', async () => {
    mockNotifications([]);
    server.use(
      http.get('/api/v1/notifications', () =>
        HttpResponse.json(
          { status: 'error', code: 'INTERNAL_ERROR', message: '알림을 불러오지 못했습니다.' },
          { status: 500 },
        ),
      ),
    );
    const user = userEvent.setup();
    renderRoute('/');

    await user.click(await screen.findByRole('button', { name: '알림' }));

    const panel = screen.getByRole('region', { name: '알림' });
    expect(await within(panel).findByRole('alert')).toBeInTheDocument();
  });

  it('알림을 누르면 읽음 처리하고 대상 피드로 이동한다', async () => {
    const reads = mockNotifications([notification({ notificationId: 7, feedId: 3 })]);
    const user = userEvent.setup();
    const router = renderRoute('/');

    await user.click(await screen.findByRole('button', { name: /^알림/ }));
    await user.click(await screen.findByRole('button', { name: /새로운 답변이 달렸어요/ }));

    await waitFor(() => expect(reads).toEqual(['7']));
    await waitFor(() => expect(router.state.location.pathname).toBe('/feeds/3'));
    expect(screen.queryByRole('region', { name: '알림' })).not.toBeInTheDocument();
  });

  it('이미 읽은 알림은 다시 읽음 처리하지 않는다', async () => {
    const reads = mockNotifications([notification({ notificationId: 7, isRead: true })]);
    const user = userEvent.setup();
    const router = renderRoute('/');

    await user.click(await screen.findByRole('button', { name: '알림' }));
    await user.click(await screen.findByRole('button', { name: /새로운 답변이 달렸어요/ }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/feeds/1'));
    expect(reads).toEqual([]);
  });

  it('모두 읽음을 누르면 전체 읽음 처리를 요청한다', async () => {
    const reads = mockNotifications([notification()]);
    const user = userEvent.setup();
    renderRoute('/');

    await user.click(await screen.findByRole('button', { name: /^알림/ }));
    await user.click(await screen.findByRole('button', { name: '모두 읽음' }));

    await waitFor(() => expect(reads).toEqual(['all']));
  });

  it('Escape를 누르면 알림 박스를 닫고 버튼으로 초점을 돌린다', async () => {
    mockNotifications([]);
    const user = userEvent.setup();
    renderRoute('/');

    const button = await screen.findByRole('button', { name: '알림' });
    await user.click(button);
    expect(screen.getByRole('region', { name: '알림' })).toBeInTheDocument();

    await user.keyboard('{Escape}');

    expect(screen.queryByRole('region', { name: '알림' })).not.toBeInTheDocument();
    expect(button).toHaveFocus();
  });
});
