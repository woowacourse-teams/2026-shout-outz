import { mutationOptions, queryOptions } from '@tanstack/react-query';
import type {
  NotificationListSuccessResponse,
  NotificationReadSuccessResponse,
  NotificationUnreadCountSuccessResponse,
} from '@/api/generated/schema';
import type { NotificationItem } from '@/types/notification';
import { httpClient } from '@/utils/client';

/** 알림 박스에 한 번에 보여줄 개수. 서버 허용 범위는 1~50. */
const NOTIFICATION_LIST_SIZE = 30;

/** 읽지 않은 알림 수를 다시 묻는 주기. 실시간 연결이 없어 주기적으로 조회한다. */
const UNREAD_COUNT_POLL_INTERVAL = 45_000;

export async function fetchNotifications(signal?: AbortSignal): Promise<NotificationItem[]> {
  const response = await httpClient<NotificationListSuccessResponse>('/api/v1/notifications', {
    method: 'get',
    signal,
    searchParams: { size: NOTIFICATION_LIST_SIZE },
  });

  if (!response) throw new Error('알림 목록 응답이 비어 있습니다.');
  return response.data;
}

export async function fetchUnreadNotificationCount(signal?: AbortSignal) {
  const response = await httpClient<NotificationUnreadCountSuccessResponse>(
    '/api/v1/notifications/unread-count',
    { method: 'get', signal },
  );

  if (!response) throw new Error('읽지 않은 알림 수 응답이 비어 있습니다.');
  return response.data.unreadCount;
}

export async function markNotificationRead(notificationId: number) {
  const response = await httpClient<NotificationReadSuccessResponse>(
    `/api/v1/notifications/${notificationId}/read`,
    { method: 'patch' },
  );

  if (!response) throw new Error('알림 읽음 처리 응답이 비어 있습니다.');
  return response.data;
}

/** 응답 본문 없이 204로 끝난다. */
export async function markAllNotificationsRead() {
  await httpClient('/api/v1/notifications/read-all', { method: 'patch' });
}

export const notificationsQuery = queryOptions({
  queryKey: ['notifications', 'list'],
  queryFn: ({ signal }) => fetchNotifications(signal),
});

export const unreadNotificationCountQuery = queryOptions({
  queryKey: ['notifications', 'unread-count'],
  queryFn: ({ signal }) => fetchUnreadNotificationCount(signal),
  refetchInterval: UNREAD_COUNT_POLL_INTERVAL,
});

export const markNotificationReadMutation = mutationOptions({
  mutationFn: markNotificationRead,
});

export const markAllNotificationsReadMutation = mutationOptions({
  mutationFn: markAllNotificationsRead,
});
