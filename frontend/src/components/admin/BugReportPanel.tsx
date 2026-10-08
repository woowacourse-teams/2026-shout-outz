import { useState } from 'react';
import {
  useMutation,
  useQuery,
  useQueryClient,
  useSuspenseInfiniteQuery,
} from '@tanstack/react-query';

import {
  adminBugReportQuery,
  adminBugReportsQuery,
  updateBugReportStatusMutation,
} from '@/apis/admin';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { Tab } from '@/components/Tab';
import { BUG_REPORT_FILTER_LABELS, BUG_REPORT_STATUS_LABELS } from '@/constants/admin';
import type { AdminBugReport, AdminBugReportFilter } from '@/types/admin';
import { formatDateTime } from '@/utils/date';
import { getApiErrorMessage } from '@/utils/error';

const FILTERS = Object.keys(BUG_REPORT_FILTER_LABELS) as AdminBugReportFilter[];

/** 사용자가 보낸 버그 제보 확인. 접수된 제보를 보고 처리 완료로 바꾼다. */
export function BugReportPanel() {
  const [filter, setFilter] = useState<AdminBugReportFilter>('OPEN');

  return (
    <div className="flex flex-col gap-4">
      <Tab
        variant="chip"
        size="sm"
        value={filter}
        onChange={(next) => setFilter(next as AdminBugReportFilter)}
        aria-label="버그 제보 상태"
      >
        {FILTERS.map((value) => (
          <Tab.Item key={value} value={value}>
            {BUG_REPORT_FILTER_LABELS[value]}
          </Tab.Item>
        ))}
      </Tab>
      <AsyncBoundary>
        <BugReportList filter={filter} />
      </AsyncBoundary>
    </div>
  );
}

function BugReportList({ filter }: { filter: AdminBugReportFilter }) {
  const { data, hasNextPage, fetchNextPage, isFetchingNextPage } = useSuspenseInfiniteQuery(
    adminBugReportsQuery(filter),
  );
  const items = data.pages.flatMap((page) => page.data);
  const totalCount = data.pages[0]?.meta.totalCount ?? items.length;

  if (items.length === 0) {
    return (
      <p className="py-10 text-center text-sm text-gray-500">
        {filter === 'ALL'
          ? '들어온 버그 제보가 없습니다.'
          : `${BUG_REPORT_STATUS_LABELS[filter]} 상태의 버그 제보가 없습니다.`}
      </p>
    );
  }

  return (
    <>
      <p className="text-sm text-gray-600">총 {totalCount.toLocaleString()}건</p>
      <ul
        aria-label="버그 제보 목록"
        className="flex flex-col divide-y divide-gray-200 rounded-xl border border-gray-200"
      >
        {items.map((item) => (
          <BugReportRow key={item.bugReportId} item={item} />
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

function BugReportRow({ item }: { item: AdminBugReport }) {
  const client = useQueryClient();
  const [expanded, setExpanded] = useState(false);
  // 목록은 200자 미리보기만 준다. 펼칠 때만 상세를 불러온다.
  const detail = useQuery({ ...adminBugReportQuery(item.bugReportId), enabled: expanded });
  const update = useMutation({
    ...updateBugReportStatusMutation,
    onSuccess: () =>
      Promise.all([
        client.invalidateQueries({ queryKey: ['admin', 'bug-reports'] }),
        client.invalidateQueries({ queryKey: ['admin', 'bug-report', item.bugReportId] }),
      ]),
  });
  const open = item.status === 'OPEN';
  const contentId = `bug-report-${item.bugReportId}-content`;

  return (
    <li className="flex flex-col gap-3 p-4" aria-label={`버그 제보 #${item.bugReportId}`}>
      <div className="flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
        <div className="flex flex-wrap items-center gap-2 text-sm">
          <span className="font-bold">#{item.bugReportId}</span>
          <Badge tone={open ? 'primary' : 'green'}>{BUG_REPORT_STATUS_LABELS[item.status]}</Badge>
          <span className="text-gray-500">{formatDateTime(item.createdAt)}</span>
          <span className="text-gray-500">
            {item.reporterUserId == null ? '비로그인 제보' : `사용자 #${item.reporterUserId}`}
          </span>
        </div>
        <Button
          variant={open ? 'primary' : 'outline'}
          size="sm"
          className="shrink-0 self-start"
          disabled={update.isPending}
          onClick={() =>
            update.mutate({ bugReportId: item.bugReportId, status: open ? 'COMPLETED' : 'OPEN' })
          }
        >
          {open ? '처리 완료' : '다시 접수'}
        </Button>
      </div>

      <p id={contentId} className="text-sm leading-relaxed break-words whitespace-pre-wrap">
        {expanded && detail.data ? detail.data.content : item.contentPreview}
      </p>
      <div className="flex flex-wrap items-center gap-3">
        <Button
          variant="ghost"
          size="sm"
          aria-expanded={expanded}
          aria-controls={contentId}
          onClick={() => setExpanded((previous) => !previous)}
        >
          {expanded ? '접기' : '전체 내용 보기'}
        </Button>
        {expanded && detail.isPending && (
          <span role="status" className="text-xs text-gray-500">
            불러오는 중…
          </span>
        )}
        {item.statusChangedAt && (
          <span className="text-xs text-gray-500">
            상태 변경 {formatDateTime(item.statusChangedAt)}
          </span>
        )}
      </div>
      {(detail.isError || update.isError) && (
        <p role="alert" className="text-xs text-red-600">
          {getApiErrorMessage(detail.error ?? update.error)}
        </p>
      )}
    </li>
  );
}
