import { useState } from 'react';
import {
  useMutation,
  useQuery,
  useQueryClient,
  useSuspenseInfiniteQuery,
} from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';

import {
  adminProjectDetailQuery,
  adminProjectsQuery,
  approveProjectMutation,
  rejectProjectMutation,
} from '@/apis/admin';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { MarkdownContent } from '@/components/MarkdownContent';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { ReviewActions } from '@/components/admin/ReviewActions';
import { ReviewStatusTab } from '@/components/admin/ReviewStatusTab';
import { REVIEW_STATUS_LABELS } from '@/constants/admin';
import type { AdminProject, AdminProjectStatus } from '@/types/admin';
import { toProjectSlugParam } from '@/utils/project';

/** 프로젝트 등록 심사. */
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
  const [detailsOpen, setDetailsOpen] = useState(false);
  const detailId = `admin-project-detail-${item.id}`;
  const detail = useQuery({ ...adminProjectDetailQuery(item.id), enabled: detailsOpen });
  const onDecisionSuccess = () => {
    void client.invalidateQueries({ queryKey: ['admin', 'projects'] });
  };
  const onApproveSuccess = () => {
    onDecisionSuccess();
    void client.invalidateQueries({ queryKey: ['project-list'] });
    void client.invalidateQueries({ queryKey: ['home', 'statistics'] });
    void client.invalidateQueries({ queryKey: ['users'] });
  };
  const approve = useMutation({ ...approveProjectMutation, onSuccess: onApproveSuccess });
  const reject = useMutation({ ...rejectProjectMutation, onSuccess: onDecisionSuccess });

  return (
    <li className="flex flex-col gap-3 p-4 md:flex-row md:items-center md:justify-between">
      <div className="flex min-w-0 flex-col gap-1">
        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            aria-expanded={detailsOpen}
            aria-controls={detailId}
            onClick={() => setDetailsOpen((open) => !open)}
            className="focus-visible:outline-primary-600 cursor-pointer rounded-sm font-bold hover:underline focus-visible:outline-2"
          >
            {item.title} 상세 보기
          </button>
          <Badge tone="primary">{item.cohort}기</Badge>
        </div>
        <p className="truncate text-sm text-gray-600">{item.tagline}</p>
        <p className="text-xs text-gray-500">
          {item.members.map((member) => member.displayName).join(', ')}
        </p>
        {item.rejectReason && (
          <p className="text-xs text-red-600">반려 사유: {item.rejectReason}</p>
        )}
        <div id={detailId} hidden={!detailsOpen}>
          {detailsOpen && (
            <div className="mt-3 space-y-2 border-t border-gray-200 pt-3 text-sm">
              {detail.isPending && <p>프로젝트 상세를 불러오는 중…</p>}
              {detail.isError && <p role="alert">프로젝트 상세를 불러오지 못했습니다.</p>}
              {detail.data && (
                <>
                  <MarkdownContent>{detail.data.descriptionMd ?? ''}</MarkdownContent>
                  <a
                    href={detail.data.githubRepositoryUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="underline"
                  >
                    GitHub 저장소
                  </a>
                  {detail.data.deploymentUrl && (
                    <a
                      href={detail.data.deploymentUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="ml-3 underline"
                    >
                      서비스 바로가기
                    </a>
                  )}
                  {item.approvalStatus === 'APPROVED' && (
                    <Link
                      to="/projects/$slug"
                      params={{ slug: toProjectSlugParam(item.slug) }}
                      className="ml-3 underline"
                    >
                      공개 페이지
                    </Link>
                  )}
                </>
              )}
            </div>
          )}
        </div>
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
