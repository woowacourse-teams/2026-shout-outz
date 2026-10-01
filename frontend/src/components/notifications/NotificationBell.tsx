import { useEffect, useId, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';
import { IconBell } from '@tabler/icons-react';

import {
  markAllNotificationsReadMutation,
  markNotificationReadMutation,
  notificationsQuery,
  unreadNotificationCountQuery,
} from '@/apis/notification';
import { Button } from '@/components/Button';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import type { NotificationItem } from '@/types/notification';
import { cn } from '@/utils/cn';
import { formatRelativeTime } from '@/utils/date';

/** 배지에 그대로 쓰는 최대 숫자. 넘으면 `99+`로 줄인다. */
const MAX_BADGE_COUNT = 99;

/**
 * 헤더의 알림 버튼과 알림 박스. 로그인한 사용자에게만 그린다.
 *
 * 읽지 않은 수는 주기적으로 조회한다. 박스는 열 때만 그려서, 열 때마다 목록을 새로 받는다.
 * 알림을 누르면 읽음 처리하고 대상 피드로 이동한다.
 */
export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const panelId = useId();
  const titleId = useId();
  const queryClient = useQueryClient();

  // 헤더는 Suspense 경계 밖이라 개수는 Suspense 없이 조회한다. 실패하면 배지만 숨긴다.
  const unreadCount = useQuery(unreadNotificationCountQuery).data ?? 0;
  const markAllRead = useMutation({
    ...markAllNotificationsReadMutation,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  });

  useEffect(() => {
    if (!open) return;

    const onPointerDown = (event: PointerEvent) => {
      if (event.target instanceof Node && !rootRef.current?.contains(event.target)) {
        setOpen(false);
      }
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return;
      setOpen(false);
      buttonRef.current?.focus();
    };

    document.addEventListener('pointerdown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('pointerdown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [open]);

  return (
    <div ref={rootRef} className="relative mr-2 shrink-0">
      <Button
        ref={buttonRef}
        variant="ghost"
        size="sm"
        aria-label={unreadCount > 0 ? `알림, 읽지 않은 알림 ${unreadCount}개` : '알림'}
        aria-expanded={open}
        aria-controls={open ? panelId : undefined}
        onClick={() => setOpen((current) => !current)}
        className="aria-expanded:bg-primary-50 aria-expanded:text-primary-600 relative size-8 rounded-full px-0"
      >
        <IconBell className="size-4.5" stroke={1.7} aria-hidden="true" />
        {unreadCount > 0 && (
          <span
            aria-hidden="true"
            className="bg-primary-600 border-background absolute -top-1.5 -right-1.5 inline-flex h-5 min-w-5 items-center justify-center rounded-full border-2 px-1 text-xs leading-none font-medium text-white"
          >
            {unreadCount > MAX_BADGE_COUNT ? `${MAX_BADGE_COUNT}+` : unreadCount}
          </span>
        )}
      </Button>

      {open && (
        <section
          id={panelId}
          aria-labelledby={titleId}
          className="bg-background absolute top-full -right-1 z-60 mt-3 w-[min(22rem,calc(100vw-2rem))] overflow-hidden rounded-xl border border-gray-200 shadow-lg"
        >
          <div className="flex items-center justify-between gap-3 border-b border-gray-100 py-2 pr-2 pl-4">
            <h2 id={titleId} className="text-sm font-bold text-gray-900">
              알림
            </h2>
            <Button
              variant="ghost"
              size="sm"
              disabled={markAllRead.isPending}
              onClick={() => markAllRead.mutate()}
              className="text-primary-600"
            >
              모두 읽음
            </Button>
          </div>
          <div className="max-h-[min(26rem,calc(100dvh-10rem))] overflow-y-auto">
            <AsyncBoundary fallback={<NotificationMessage>불러오는 중…</NotificationMessage>}>
              <NotificationList onNavigate={() => setOpen(false)} />
            </AsyncBoundary>
          </div>
        </section>
      )}
    </div>
  );
}

function NotificationList({ onNavigate }: { onNavigate: () => void }) {
  const { data: notifications } = useSuspenseQuery(notificationsQuery);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const markRead = useMutation({
    ...markNotificationReadMutation,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  });

  if (notifications.length === 0) {
    return <NotificationMessage>새로운 활동이 없어요.</NotificationMessage>;
  }

  const open = (notification: NotificationItem) => {
    if (!notification.isRead) markRead.mutate(notification.notificationId);
    onNavigate();
    if (notification.feedId != null) {
      void navigate({ to: '/feeds/$feedId', params: { feedId: String(notification.feedId) } });
    }
  };

  return (
    <ul>
      {notifications.map((notification) => (
        <li key={notification.notificationId} className="border-b border-gray-100 last:border-b-0">
          <button
            type="button"
            onClick={() => open(notification)}
            className={cn(
              'flex w-full cursor-pointer gap-3 px-4 py-3 text-left transition-colors focus-visible:outline-none',
              notification.isRead
                ? 'hover:bg-gray-50 focus-visible:bg-gray-50'
                : 'bg-primary-50 hover:bg-primary-100 focus-visible:bg-primary-100',
            )}
          >
            {/* 읽음 여부를 점으로 표시한다. 읽은 알림도 자리를 비워 두어 글자 시작선을 맞춘다. */}
            <span
              aria-hidden="true"
              className={cn(
                'mt-1.5 size-2 shrink-0 rounded-full',
                !notification.isRead && 'bg-primary-600',
              )}
            />
            <span className="grid min-w-0 flex-1 gap-1">
              {!notification.isRead && <span className="sr-only">읽지 않음</span>}
              <span
                className={cn(
                  'text-sm leading-snug',
                  notification.isRead ? 'font-medium text-gray-500' : 'font-semibold text-gray-900',
                )}
              >
                {notification.message}
              </span>
              {notification.feedTitle && (
                <span className="truncate text-xs leading-snug text-gray-500">
                  {notification.feedTitle}
                </span>
              )}
              <span className="text-xs text-gray-400">
                {formatRelativeTime(notification.createdAt)}
              </span>
            </span>
          </button>
        </li>
      ))}
    </ul>
  );
}

function NotificationMessage({ children }: { children: string }) {
  return <p className="px-5 py-8 text-center text-sm text-gray-400">{children}</p>;
}
