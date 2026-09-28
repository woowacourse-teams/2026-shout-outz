import { useState } from 'react';
import { useMutation, useQueryClient, useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';

import { adminProjectsQuery, approveProjectMutation, rejectProjectMutation } from '@/apis/admin';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { ReviewActions } from '@/components/admin/ReviewActions';
import { ReviewStatusTab } from '@/components/admin/ReviewStatusTab';
import { REVIEW_STATUS_LABELS } from '@/constants/admin';
import type { AdminProject, AdminProjectStatus } from '@/types/admin';
import { toProjectSlugParam } from '@/utils/project';

/**
 * 프로젝트 등록 심사.
 *
 * TODO 관리자 프로젝트 심사 API가 명세에 없다. `src/apis/admin.ts`의 가정한 주소로 요청하므로
 * 서버가 붙기 전까지는 목록을 불러오지 못한다.
 */
export function ProjectApprovalPanel() {
  const [status, setStatus] = useState<AdminProjectStatus>('PENDING');

  return (
    <div className="flex flex-col gap-4">
      <ReviewStatusTab value={status} onChange={setStatus} />
      <AsyncBoundary>
        <ProjectList status={status} />
      </AsyncBoundary>
    </div>
  );
}

function ProjectList({ status }: { status: AdminProjectStatus }) {
  const { data, hasNextPage, fetchNextPage, isFetchingNextPage } = useSuspenseInfiniteQuery(
    adminProjectsQuery(status),
  );
  const items = data.pages.flatMap((page) => page.items);

  if (items.length === 0) {
    return (
      <p className="py-10 text-center text-sm text-gray-500">
        {REVIEW_STATUS_LABELS[status]} 상태의 프로젝트가 없습니다.
      </p>
    );
  }

  return (
    <>
      <ul className="flex flex-col divide-y divide-gray-200 rounded-xl border border-gray-200">
        {items.map((item) => (
          <ProjectRow key={item.id} item={item} />
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

function ProjectRow({ item }: { item: AdminProject }) {
  const client = useQueryClient();
  const onSuccess = () => client.invalidateQueries({ queryKey: ['admin', 'projects'] });
  const approve = useMutation({ ...approveProjectMutation, onSuccess });
  const reject = useMutation({ ...rejectProjectMutation, onSuccess });

  return (
    <li className="flex flex-col gap-3 p-4 md:flex-row md:items-center md:justify-between">
      <div className="flex min-w-0 flex-col gap-1">
        <div className="flex flex-wrap items-center gap-2">
          {/* 승인 전 프로젝트 상세는 등록자만 볼 수 있어, 관리자 조회가 열리기 전까지는 404일 수 있다. */}
          <Link
            to="/projects/$slug"
            params={{ slug: toProjectSlugParam(item.slug) }}
            className="font-bold hover:underline"
          >
            {item.title}
          </Link>
          <Badge tone="primary">{item.cohort}기</Badge>
        </div>
        <p className="truncate text-sm text-gray-600">{item.tagline}</p>
        <p className="text-xs text-gray-500">
          {item.members.map((member) => member.displayName).join(', ')}
        </p>
        {item.rejectReason && (
          <p className="text-xs text-red-600">반려 사유: {item.rejectReason}</p>
        )}
      </div>
      {item.approvalStatus === 'PENDING' && (
        <ReviewActions
          subject={item.title}
          pending={approve.isPending || reject.isPending}
          error={approve.error ?? reject.error}
          onApprove={() => approve.mutate(item.id)}
          onReject={(reason) => reject.mutate({ projectId: item.id, reason })}
        />
      )}
    </li>
  );
}
