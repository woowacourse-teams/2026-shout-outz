import { useState } from 'react';
import { useMutation, useQueryClient, useSuspenseInfiniteQuery } from '@tanstack/react-query';

import {
  adminVerificationsQuery,
  approveVerificationMutation,
  rejectVerificationMutation,
} from '@/apis/admin';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { ReviewActions } from '@/components/admin/ReviewActions';
import { ReviewStatusTab } from '@/components/admin/ReviewStatusTab';
import { REVIEW_STATUS_LABELS } from '@/constants/admin';
import type { AdminVerification, AdminVerificationStatus } from '@/types/admin';
import { formatDotDate } from '@/utils/date';
import { formatCrewRole } from '@/utils/user';

const USER_TYPE_LABELS: Record<AdminVerification['userType'], string> = {
  GENERAL: '일반',
  WOOWACOURSE_CREW: '크루',
  WOOWACOURSE_COACH: '코치',
};

/** 크루/코치 인증 신청 심사. */
export function CrewApprovalPanel() {
  const [status, setStatus] = useState<AdminVerificationStatus>('PENDING');

  return (
    <div className="flex flex-col gap-4">
      <ReviewStatusTab value={status} onChange={setStatus} />
      <AsyncBoundary>
        <VerificationList status={status} />
      </AsyncBoundary>
    </div>
  );
}

function VerificationList({ status }: { status: AdminVerificationStatus }) {
  const { data, hasNextPage, fetchNextPage, isFetchingNextPage } = useSuspenseInfiniteQuery(
    adminVerificationsQuery(status),
  );
  const items = data.pages.flatMap((page) => page.data);

  if (items.length === 0) {
    return (
      <p className="py-10 text-center text-sm text-gray-500">
        {REVIEW_STATUS_LABELS[status]} 상태의 인증 신청이 없습니다.
      </p>
    );
  }

  return (
    <>
      <ul className="flex flex-col divide-y divide-gray-200 rounded-xl border border-gray-200">
        {items.map((item) => (
          <VerificationRow key={item.requestId} item={item} />
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

function VerificationRow({ item }: { item: AdminVerification }) {
  const client = useQueryClient();
  const onSuccess = () =>
    client.invalidateQueries({ queryKey: ['admin', 'verification-requests'] });
  const approve = useMutation({ ...approveVerificationMutation, onSuccess });
  const reject = useMutation({ ...rejectVerificationMutation, onSuccess });
  const role = formatCrewRole(item.cohort, item.track);

  return (
    <li className="flex flex-col gap-3 p-4 md:flex-row md:items-center md:justify-between">
      <div className="flex flex-col gap-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-bold">{item.nickname}</span>
          <span className="text-sm text-gray-500">@{item.applicant.handle}</span>
          <Badge tone="primary">{USER_TYPE_LABELS[item.userType]}</Badge>
          {role && <Badge>{role}</Badge>}
        </div>
        <p className="text-xs text-gray-500">신청일 {formatDotDate(item.requestedAt)}</p>
      </div>
      {item.status === 'PENDING' && (
        <ReviewActions
          subject={item.nickname}
          pending={approve.isPending || reject.isPending}
          error={approve.error ?? reject.error}
          onApprove={() => approve.mutate(item.requestId)}
          onReject={(reason) => reject.mutate({ requestId: item.requestId, reason })}
        />
      )}
    </li>
  );
}
