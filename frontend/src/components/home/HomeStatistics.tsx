import { useSuspenseQuery } from '@tanstack/react-query';
import { homeStatisticsQueryOptions } from '@/api/home';
import { cn } from '@/utils/cn';

const numberFormat = new Intl.NumberFormat('ko-KR');

export function HomeStatistics() {
  const { data: statistics } = useSuspenseQuery(homeStatisticsQueryOptions());

  const items = [
    {
      label: '아카이빙된 프로젝트',
      value: statistics.projectCount,
      unit: '+',
      tone: 'text-primary-600',
    },
    {
      label: '기술 글 · 회고 링크',
      value: statistics.feedCount,
      unit: '개',
      tone: 'text-primary-600',
    },
    {
      label: '우아한테크코스 기수',
      value: statistics.currentCohort,
      unit: '기수',
      tone: 'text-gray-900',
    },
    {
      label: '진행 중 크루 이벤트',
      value: statistics.ongoingEventCount,
      unit: '건',
      tone: 'text-gray-600',
    },
  ];

  return (
    <section
      aria-label="서비스 통계"
      className="rounded-xl border border-gray-200 bg-gray-50 px-5 py-4 md:px-9"
    >
      <ul className="grid grid-cols-2 gap-3 md:grid-cols-4 md:gap-0 md:divide-x md:divide-gray-200">
        {items.map(({ label, value, unit, tone }) => (
          <li key={label} className="flex flex-col gap-0.5 md:px-9 md:first:pl-0 md:last:pr-0">
            <p className="flex items-end gap-0.5">
              <span className={cn('text-2xl font-bold tracking-tight', tone)}>
                {numberFormat.format(value)}
              </span>
              <span className="pb-0.5 text-sm font-bold text-gray-500">{unit}</span>
            </p>
            <p className="text-xs text-gray-600">{label}</p>
          </li>
        ))}
      </ul>
    </section>
  );
}
