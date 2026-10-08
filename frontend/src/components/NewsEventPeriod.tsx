import { formatDateTime, formatDotDate } from '@/utils/date';

interface NewsEventPeriodProps {
  startAt?: string | null;
  endAt?: string | null;
  compact?: boolean;
}

export function NewsEventPeriod({ startAt, endAt, compact = false }: NewsEventPeriodProps) {
  if (!startAt && !endAt) return null;

  if (compact) {
    const start = startAt ? formatDotDate(startAt) : null;
    const end = endAt ? formatDotDate(endAt) : null;
    const omitYear = start && end && start.slice(0, 4) === end.slice(0, 4);
    return (
      <div className="flex flex-wrap items-baseline gap-x-2 gap-y-1 text-xs leading-5 text-gray-500">
        <span className="shrink-0">이벤트 기간</span>
        <p className="flex flex-wrap gap-x-1.5">
          {startAt && start ? (
            <time dateTime={startAt} title={formatDateTime(startAt)}>
              {omitYear ? start.slice(5) : start}
            </time>
          ) : (
            <span>시작일 미정</span>
          )}
          <span>~</span>
          {endAt && end ? (
            <time dateTime={endAt} title={formatDateTime(endAt)}>
              {omitYear ? end.slice(5) : end}
            </time>
          ) : (
            <span>종료일 미정</span>
          )}
        </p>
      </div>
    );
  }

  return (
    <dl className="grid gap-2 text-sm">
      <div className="flex flex-wrap items-baseline gap-x-4 gap-y-1">
        <dt className="shrink-0 text-gray-500">시작</dt>
        <dd className="font-medium text-gray-900">
          {startAt ? <time dateTime={startAt}>{formatDateTime(startAt)}</time> : '시작일 미정'}
        </dd>
      </div>
      <div className="flex flex-wrap items-baseline gap-x-4 gap-y-1">
        <dt className="shrink-0 text-gray-500">종료</dt>
        <dd className="font-medium text-gray-900">
          {endAt ? <time dateTime={endAt}>{formatDateTime(endAt)}</time> : '종료일 미정'}
        </dd>
      </div>
    </dl>
  );
}
