import { http, HttpResponse } from 'msw';
import type { NotificationItem } from '@/types/notification';

/** 알림 목 데이터. 읽음 처리가 화면에 반영되도록 핸들러가 이 배열을 고쳐 쓴다. */
const notifications: NotificationItem[] = [
  {
    notificationId: 3,
    notificationType: 'QUESTION_ACTIVITY',
    message: '새로운 답변이 달렸어요.',
    feedId: 1,
    feedTitle: 'WebSocket 동기화 개선기',
    commentId: 10,
    actor: { userId: 2, handle: 'crew1', displayName: '김도현', avatarUrl: null },
    isRead: false,
    createdAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
  },
  {
    notificationId: 2,
    notificationType: 'QUESTION_ACTIVITY',
    message: '새로운 답변이 달렸어요.',
    feedId: 3,
    feedTitle: '기억에 남는 트러블슈팅',
    commentId: 30,
    actor: { userId: 3, handle: 'crew2', displayName: '이지민', avatarUrl: null },
    isRead: false,
    createdAt: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(),
  },
  {
    notificationId: 1,
    notificationType: 'QUESTION_ACTIVITY',
    message: '새로운 답변이 달렸어요.',
    feedId: 2,
    feedTitle: 'TanStack Query 서버 상태 관리',
    commentId: 20,
    actor: { userId: 2, handle: 'crew1', displayName: '김도현', avatarUrl: null },
    isRead: true,
    createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
  },
];

const unreadCount = () => notifications.filter((item) => !item.isRead).length;

export const notificationHandlers = [
  http.get('/api/v1/notifications', () =>
    HttpResponse.json({
      status: 'success',
      data: notifications,
      meta: { hasNext: false, nextCursor: null, totalCount: notifications.length },
    }),
  ),
  http.get('/api/v1/notifications/unread-count', () =>
    HttpResponse.json({ status: 'success', data: { unreadCount: unreadCount() } }),
  ),
  http.patch('/api/v1/notifications/read-all', () => {
    notifications.forEach((item) => (item.isRead = true));
    return new HttpResponse(null, { status: 204 });
  }),
  http.patch('/api/v1/notifications/:notificationId/read', ({ params }) => {
    const notificationId = Number(params.notificationId);
    const target = notifications.find((item) => item.notificationId === notificationId);
    if (!target) {
      return HttpResponse.json(
        { status: 'error', code: 'NOT_FOUND', message: '알림을 찾을 수 없습니다.' },
        { status: 404 },
      );
    }
    target.isRead = true;
    return HttpResponse.json({ status: 'success', data: { notificationId, isRead: true } });
  }),
];
