import { useState } from 'react';
import { useMutation, useQueryClient, useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';

import { adminResourceReportsQuery, updateResourceReportStatusMutation } from '@/apis/admin';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { Tab } from '@/components/Tab';
import {
  RESOURCE_REPORT_FILTER_LABELS,
  RESOURCE_REPORT_KIND_LABELS,
  RESOURCE_REPORT_STATUS_LABELS,
} from '@/constants/admin';
import type {
  AdminResourceReport,
  AdminResourceReportFilter,
  AdminResourceReportKind,
  AdminResourceReportStatus,
} from '@/types/admin';
import { formatDateTime } from '@/utils/date';
import { getApiErrorMessage } from '@/utils/error';

const FILTERS = Object.keys(RESOURCE_REPORT_FILTER_LABELS) as AdminResourceReportFilter[];
const KINDS = ['ALL', ...Object.keys(RESOURCE_REPORT_KIND_LABELS)] as (
  AdminResourceReportKind | 'ALL'
)[];

const STATUS_TONE = {
  OPEN: 'primary',
  ACCEPTED: 'green',
  REJECTED: 'gray',
} as const satisfies Record<AdminResourceReportStatus, string>;

/**
 * 프로젝트 팀원이 보낸 자료 제보 처리. 버그 제보 패널과 같은 모양이다.
 *
 * - 필요 없어요: 반영하면 서버가 그 자료를 기술 검색과 추천에서 뺀다.
 * - 빠진 자료: 알려 준 GitHub 주소를 확인하고 반영 여부만 기록한다.
 */
export function ResourceReportPanel() {
  const [filter, setFilter] = useState<AdminResourceReportFilter>('OPEN');
  const [kind, setKind] = useState<AdminResourceReportKind | 'ALL'>('ALL');

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-2">
        <Tab
          variant="chip"
          size="sm"
          value={filter}
          onChange={(next) => setFilter(next as AdminResourceReportFilter)}
          aria-label="자료 제보 상태"
        >
          {FILTERS.map((value) => (
            <Tab.Item key={value} value={value}>
              {RESOURCE_REPORT_FILTER_LABELS[value]}
            </Tab.Item>
          ))}
        </Tab>
        <Tab
          variant="chip"
          size="sm"
          value={kind}
          onChange={(next) => setKind(next as AdminResourceReportKind | 'ALL')}
          aria-label="자료 제보 종류"
        >
          {KINDS.map((value) => (
            <Tab.Item key={value} value={value}>
              {value === 'ALL' ? '모든 종류' : RESOURCE_REPORT_KIND_LABELS[value]}
            </Tab.Item>
          ))}
        </Tab>
      </div>
      <AsyncBoundary>
        <ResourceReportList filter={filter} kind={kind === 'ALL' ? null : kind} />
      </AsyncBoundary>
    </div>
  );
}

function ResourceReportList({
  filter,
  kind,
}: {
  filter: AdminResourceReportFilter;
  kind: AdminResourceReportKind | null;
}) {
  const { data, hasNextPage, fetchNextPage, isFetchingNextPage } = useSuspenseInfiniteQuery(
    adminResourceReportsQuery(filter, kind),
  );
  const items = data.pages.flatMap((page) => page.data);
  const totalCount = data.pages[0]?.meta.totalCount ?? items.length;

  if (items.length === 0) {
    return (
      <p className="py-10 text-center text-sm text-gray-500">
        {filter === 'ALL'
          ? '들어온 자료 제보가 없습니다.'
          : `${RESOURCE_REPORT_STATUS_LABELS[filter]} 상태의 자료 제보가 없습니다.`}
      </p>
    );
  }

  return (
    <>
      <p className="text-sm text-gray-600">총 {totalCount.toLocaleString()}건</p>
      <ul
        aria-label="자료 제보 목록"
        className="flex flex-col divide-y divide-gray-200 rounded-xl border border-gray-200"
      >
        {items.map((item) => (
          <ResourceReportRow key={item.reportId} item={item} />
        ))}
      </ul>
      {hasNextPage && (
        <Button
          variant="outline"
          className="self-center"
          disabled={isFetchingNextPage}
          onClick={() => fetchNextPage()}
        >
          더 보기
        </Button>
      )}
    </>
  );
}

function ResourceReportRow({ item }: { item: AdminResourceReport }) {
  const client = useQueryClient();
  const update = useMutation({
    ...updateResourceReportStatusMutation,
    onSuccess: () => client.invalidateQueries({ queryKey: ['admin', 'resource-reports'] }),
  });
  const change = (status: AdminResourceReportStatus) =>
    update.mutate({ reportId: item.reportId, status });
  // 필요 없어요는 고른 자료를, 빠진 자료는 알려 준 주소를 보여 준다.
  const target = item.kind === 'REMOVE' ? item.resource : null;
  const targetUrl = target?.url ?? item.url ?? null;

  return (
    <li className="flex flex-col gap-3 p-4" aria-label={`자료 제보 #${item.reportId}`}>
      <div className="flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
        <div className="flex flex-wrap items-center gap-2 text-sm">
          <span className="font-bold">#{item.reportId}</span>
          <Badge tone={item.kind === 'REMOVE' ? 'gray' : 'primary'}>
            {RESOURCE_REPORT_KIND_LABELS[item.kind]}
          </Badge>
          <Badge tone={STATUS_TONE[item.status]}>
            {RESOURCE_REPORT_STATUS_LABELS[item.status]}
          </Badge>
          <Link
            to="/projects/$slug/materials"
            params={{ slug: `@${item.project.slug}` }}
            className="font-semibold text-gray-700 hover:underline"
          >
            {item.project.title}
          </Link>
          <span className="text-gray-500">{formatDateTime(item.createdAt)}</span>
          <span className="text-gray-500">사용자 #{item.reporterUserId}</span>
        </div>
        <div className="flex shrink-0 gap-2 self-start">
          {item.status === 'OPEN' ? (
            <>
              <Button
                variant="outline"
                size="sm"
                disabled={update.isPending}
                onClick={() => change('REJECTED')}
              >
                반영 안 함
              </Button>
              <Button size="sm" disabled={update.isPending} onClick={() => change('ACCEPTED')}>
                반영
              </Button>
            </>
          ) : (
            <Button
              variant="outline"
              size="sm"
              disabled={update.isPending}
              onClick={() => change('OPEN')}
            >
              다시 접수
            </Button>
          )}
        </div>
      </div>

      {item.kind === 'REMOVE' && !target && (
        <p className="text-sm text-gray-500">이미 내려간 자료예요.</p>
      )}
      {targetUrl && (
        <a
          href={targetUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="text-sm break-all text-gray-900 underline decoration-gray-300 underline-offset-3 hover:decoration-gray-900"
        >
          {target?.title ?? targetUrl} ↗
        </a>
      )}
      {item.memo && (
        <p className="rounded-lg bg-gray-50 px-3 py-2 text-sm leading-relaxed break-words whitespace-pre-wrap text-gray-700">
          {item.memo}
        </p>
      )}
      {update.isError && (
        <p role="alert" className="text-xs text-red-600">
          {getApiErrorMessage(update.error)}
        </p>
      )}
    </li>
  );
}
