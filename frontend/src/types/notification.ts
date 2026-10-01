import type { NotificationItemData } from '@/types/api';

/**
 * 알림 한 건. `GET /api/v1/notifications`의 항목이다.
 *
 * DOM 전역 `Notification`과 겹치지 않도록 `NotificationItem`이라 부른다.
 */
export type NotificationItem = NotificationItemData;
