import { cn } from '@/utils/cn';
import { formatDateTime, formatDotDate } from '@/utils/date';

interface NewsEventPeriodProps {
  startAt?: string | null;
  endAt?: string | null;
  compact?: boolean;
  /** compact에서 날짜를 굵게 보여 준다. 새 디자인 소식 목록에서 쓴다. */
  strongDates?: boolean;
}

export function NewsEventPeriod({
  startAt,
  endAt,
  compact = false,
  strongDates = false,
}: NewsEventPeriodProps) {
  if (!startAt && !endAt) return null;

  if (compact) {
    const start = startAt ? formatDotDate(startAt) : null;
    const end = endAt ? formatDotDate(endAt) : null;
    const omitYear = start && end && start.slice(0, 4) === end.slice(0, 4);
    return (
      <div className="flex flex-wrap items-baseline gap-x-2 gap-y-1 text-xs leading-5 text-gray-500">
        <span className="shrink-0">이벤트 기간</span>
        <p className={cn('flex flex-wrap gap-x-1.5', strongDates && 'font-semibold text-gray-600')}>
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
    <dl className="grid grid-cols-[3.5rem_minmax(0,1fr)] gap-y-1.5 text-sm">
      <dt className="text-gray-500">시작</dt>
      <dd className="font-semibold text-gray-900">
        {startAt ? <time dateTime={startAt}>{formatDateTime(startAt)}</time> : '시작일 미정'}
      </dd>
      <dt className="text-gray-500">종료</dt>
      <dd className="font-semibold text-gray-900">
        {endAt ? <time dateTime={endAt}>{formatDateTime(endAt)}</time> : '종료일 미정'}
      </dd>
    </dl>
  );
}
