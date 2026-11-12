import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import { REVIEW_STATUS_LABELS } from '@/constants/admin';
import { formatDotDate } from '@/utils/date';
import { getApiErrorMessage } from '@/utils/error';

type ReviewStatus = keyof typeof REVIEW_STATUS_LABELS;

export interface ReviewHistoryItem {
  historyId: number;
  changedAt: string;
  fromStatus?: string | null;
  toStatus: ReviewStatus;
  reason?: string | null;
  /** 프로젝트 이력은 명세에 모양이 없어 object로 온다. handle이 있으면 보여 준다. */
  changedBy?: object | null;
}

const statusLabel = (status: string | null | undefined) =>
  status && status in REVIEW_STATUS_LABELS
    ? REVIEW_STATUS_LABELS[status as ReviewStatus]
    : (status ?? '없음');

const changedByHandle = (changedBy: object | null | undefined) =>
  changedBy && 'handle' in changedBy && typeof changedBy.handle === 'string'
    ? changedBy.handle
    : null;

/**
 * 심사 항목의 상태 변경 이력. 누르면 그때 불러온다.
 * 우아한테크코스 소속 인증과 프로젝트 심사가 함께 쓴다.
 */
export function ReviewHistory({
  subject,
  queryKey,
  load,
}: {
  subject: string;
  queryKey: readonly unknown[];
  load: () => Promise<ReviewHistoryItem[]>;
}) {
  const [open, setOpen] = useState(false);
  const history = useQuery({ queryKey, queryFn: load, enabled: open });

  return (
    <div className="text-xs">
      <button
        type="button"
        aria-expanded={open}
        aria-label={`${subject} 심사 이력 ${open ? '닫기' : '보기'}`}
        onClick={() => setOpen((prev) => !prev)}
        className="focus-visible:outline-primary-600 cursor-pointer rounded-sm text-gray-500 underline hover:text-gray-900 focus-visible:outline-2"
      >
        심사 이력 {open ? '닫기' : '보기'}
      </button>
      {open && (
        <div className="mt-2">
          {history.isPending && <p className="text-gray-500">이력을 불러오는 중…</p>}
          {history.isError && (
            <p role="alert" className="text-red-600">
              {getApiErrorMessage(history.error)}
            </p>
          )}
          {history.data?.length === 0 && <p className="text-gray-500">이력이 없습니다.</p>}
          {history.data && history.data.length > 0 && (
            <ol className="flex flex-col gap-1 border-l border-gray-200 pl-3">
              {history.data.map((item) => {
                const handle = changedByHandle(item.changedBy);
                return (
                  <li key={item.historyId} className="text-gray-700">
                    <span className="text-gray-500">{formatDotDate(item.changedAt)}</span>{' '}
                    {statusLabel(item.fromStatus)} → {statusLabel(item.toStatus)}
                    {handle && <span className="text-gray-500"> · @{handle}</span>}
                    {item.reason && <span className="text-gray-500"> · {item.reason}</span>}
                  </li>
                );
              })}
            </ol>
          )}
        </div>
      )}
    </div>
  );
}
