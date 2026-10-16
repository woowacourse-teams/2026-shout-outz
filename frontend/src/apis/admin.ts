import { infiniteQueryOptions, mutationOptions, queryOptions } from '@tanstack/react-query';
import { httpClient, type ApiSuccessBody } from '@/utils/client';
import type {
  AdminProjectMigrationUpdateSuccessResponse,
  AdminVerificationRequestApproveSuccessResponse,
  AdminVerificationRequestFindAllSuccessResponse,
  AdminVerificationRequestRejectSuccessResponse,
  BugReportAdminDetailSuccessResponse,
  BugReportAdminFindAllSuccessResponse,
  BugReportStatusUpdateSuccessResponse,
  EventCreateSuccessResponse,
  HomeBannerAdminFindAllSuccessResponse,
  HomeBannerAdminSaveSuccessResponse,
  HomeBannerAdminUpdateSuccessResponse,
  HomeBannerDeleteSuccessResponse,
  InterviewAdminResponse,
  InterviewFindAllSuccessResponse,
  NoticeCreateSuccessResponse,
} from '@/api/generated/schema';
import type {
  AdminBugReportFilter,
  AdminBugReportStatus,
  AdminProject,
  AdminProjectDecision,
  AdminProjectDetail,
  AdminProjectRejection,
  AdminProjectStatus,
  AdminProjectUpdateBody,
  AdminVerificationStatus,
  EventCreateBody,
  HomeBannerUpsertBody,
  InterviewSaveBody,
  NoticeCreateBody,
} from '@/types/admin';
import type { ProjectListMeta } from '@/types/project';

const VERIFICATION_PATH = '/api/v1/admin/verification-requests';
const BANNER_PATH = '/api/v1/admin/home/banners';
const PROJECT_PATH = '/api/v1/admin/projects';
const INTERVIEW_PATH = '/api/v1/admin/interviews';

export const adminQueryKeys = {
  verifications: (status: AdminVerificationStatus) =>
    ['admin', 'verification-requests', status] as const,
  projects: (status: AdminProjectStatus) => ['admin', 'projects', status] as const,
  banners: ['admin', 'home-banners'] as const,
  bugReports: (filter: AdminBugReportFilter) => ['admin', 'bug-reports', filter] as const,
  bugReport: (bugReportId: number) => ['admin', 'bug-report', bugReportId] as const,
  interviews: ['admin', 'interviews'] as const,
  interview: (interviewId: number) => ['admin', 'interview', interviewId] as const,
};

// ── 우아한테크코스 소속 인증 신청 ───────────────────────────────────────────────────────────

export async function fetchAdminVerifications(status: AdminVerificationStatus, cursor?: string) {
  const body = await httpClient<AdminVerificationRequestFindAllSuccessResponse>(VERIFICATION_PATH, {
    method: 'get',
    searchParams: { status, ...(cursor ? { cursor } : {}) },
  });
  if (!body) throw new Error(`인증 신청 목록 응답이 비어 있습니다: ${VERIFICATION_PATH}`);
  // 목록은 data, 커서는 meta에 있다. 피드 목록과 같은 모양이라 응답을 통째로 넘긴다.
  return body;
}

export const adminVerificationsQuery = (status: AdminVerificationStatus) =>
  infiniteQueryOptions({
    queryKey: adminQueryKeys.verifications(status),
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam }) => fetchAdminVerifications(status, pageParam),
    getNextPageParam: (last) => last.meta.nextCursor ?? undefined,
  });

export async function approveVerification(requestId: number) {
  const body = await httpClient<AdminVerificationRequestApproveSuccessResponse>(
    `${VERIFICATION_PATH}/${requestId}/approve`,
    { method: 'post' },
  );
  if (!body) throw new Error('인증 신청 승인 결과를 확인하지 못했습니다.');
  return body.data;
}

export const approveVerificationMutation = mutationOptions({
  mutationFn: approveVerification,
  retry: false,
});

export async function rejectVerification({
  requestId,
  reason,
}: {
  requestId: number;
  reason: string;
}) {
  const body = await httpClient<AdminVerificationRequestRejectSuccessResponse>(
    `${VERIFICATION_PATH}/${requestId}/reject`,
    { method: 'post', json: { reason } },
  );
  if (!body) throw new Error('인증 신청 반려 결과를 확인하지 못했습니다.');
  return body.data;
}

export const rejectVerificationMutation = mutationOptions({
  mutationFn: rejectVerification,
  retry: false,
});

// ── 프로젝트 심사 ──────────────────────────────────────────────────────────

export async function fetchAdminProjects(status: AdminProjectStatus, cursor?: string) {
  const body = await httpClient<ApiSuccessBody<AdminProject[], ProjectListMeta>>(PROJECT_PATH, {
    method: 'get',
    searchParams: { status, ...(cursor ? { cursor } : {}) },
  });
  if (!body) throw new Error(`프로젝트 심사 목록 응답이 비어 있습니다: ${PROJECT_PATH}`);
  if (!Array.isArray(body.data) || !body.meta || !('nextCursor' in body.meta)) {
    throw new Error(`프로젝트 심사 목록 응답 형식이 올바르지 않습니다: ${PROJECT_PATH}`);
  }
  return { items: body.data, meta: body.meta };
}

export const adminProjectsQuery = (status: AdminProjectStatus) =>
  infiniteQueryOptions({
    queryKey: adminQueryKeys.projects(status),
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam }) => fetchAdminProjects(status, pageParam),
    getNextPageParam: (last) => last.meta.nextCursor ?? undefined,
  });

export async function fetchAdminProjectDetail(projectId: number) {
  const path = `${PROJECT_PATH}/${projectId}`;
  const body = await httpClient<ApiSuccessBody<AdminProjectDetail>>(path, { method: 'get' });
  if (!body) throw new Error(`프로젝트 심사 상세 응답이 비어 있습니다: ${path}`);
  return body.data;
}

export const adminProjectDetailQuery = (projectId: number) =>
  queryOptions({
    queryKey: ['admin', 'projects', projectId],
    queryFn: () => fetchAdminProjectDetail(projectId),
  });

export async function approveProject(projectId: number) {
  const body = await httpClient<ApiSuccessBody<AdminProjectDecision>>(
    `${PROJECT_PATH}/${projectId}/approve`,
    { method: 'post' },
  );
  if (!body) throw new Error('프로젝트 승인 결과를 확인하지 못했습니다.');
  return body.data;
}

export const approveProjectMutation = mutationOptions({
  mutationFn: approveProject,
  retry: false,
});

export async function rejectProject({ projectId, reason }: { projectId: number; reason: string }) {
  const body = await httpClient<ApiSuccessBody<AdminProjectRejection>>(
    `${PROJECT_PATH}/${projectId}/reject`,
    { method: 'post', json: { reason } },
  );
  if (!body) throw new Error('프로젝트 반려 결과를 확인하지 못했습니다.');
  return body.data;
}

export const rejectProjectMutation = mutationOptions({
  mutationFn: rejectProject,
  retry: false,
});

export async function updateAdminProject({
  projectId,
  body: input,
}: {
  projectId: number;
  body: AdminProjectUpdateBody;
}) {
  const body = await httpClient<AdminProjectMigrationUpdateSuccessResponse>(
    `${PROJECT_PATH}/${projectId}/migration`,
    { method: 'patch', json: input },
  );
  if (!body) throw new Error('프로젝트 수정 결과를 확인하지 못했습니다.');
  return body.data;
}

export const updateAdminProjectMutation = mutationOptions({
  mutationFn: updateAdminProject,
  retry: false,
});

// ── 소식 ────────────────────────────────────────────────────────────────────

export async function createNotice(input: NoticeCreateBody) {
  const body = await httpClient<NoticeCreateSuccessResponse>('/api/v1/news/notices', {
    method: 'post',
    json: input,
  });
  if (!body) throw new Error('공지 등록 결과를 확인하지 못했습니다.');
  return body.data;
}

export const createNoticeMutation = mutationOptions({
  mutationFn: createNotice,
  retry: false,
});

export async function createEvent(input: EventCreateBody) {
  const body = await httpClient<EventCreateSuccessResponse>('/api/v1/news/events', {
    method: 'post',
    json: input,
  });
  if (!body) throw new Error('이벤트 등록 결과를 확인하지 못했습니다.');
  return body.data;
}

export const createEventMutation = mutationOptions({
  mutationFn: createEvent,
  retry: false,
});

// ── 홈 배너 ─────────────────────────────────────────────────────────────────

export async function fetchAdminBanners(signal?: AbortSignal) {
  const body = await httpClient<HomeBannerAdminFindAllSuccessResponse>(BANNER_PATH, {
    method: 'get',
    signal,
  });
  if (!body) throw new Error(`관리자 홈 배너 응답이 비어 있습니다: ${BANNER_PATH}`);
  return body.data;
}

export const adminBannersQuery = queryOptions({
  queryKey: adminQueryKeys.banners,
  queryFn: ({ signal }) => fetchAdminBanners(signal),
});

export async function createBanner(input: HomeBannerUpsertBody) {
  const body = await httpClient<HomeBannerAdminSaveSuccessResponse>(BANNER_PATH, {
    method: 'post',
    json: input,
  });
  if (!body) throw new Error('홈 배너 등록 결과를 확인하지 못했습니다.');
  return body.data;
}

export const createBannerMutation = mutationOptions({
  mutationFn: createBanner,
  retry: false,
});

export async function updateBanner({
  bannerId,
  body: input,
}: {
  bannerId: number;
  body: HomeBannerUpsertBody;
}) {
  const body = await httpClient<HomeBannerAdminUpdateSuccessResponse>(
    `${BANNER_PATH}/${bannerId}`,
    { method: 'put', json: input },
  );
  if (!body) throw new Error('홈 배너 수정 결과를 확인하지 못했습니다.');
  return body.data;
}

export const updateBannerMutation = mutationOptions({
  mutationFn: updateBanner,
  retry: false,
});

export async function deleteBanner(bannerId: number) {
  // 명세상 200 본문과 204가 모두 올 수 있어 결과는 쓰지 않는다.
  await httpClient<HomeBannerDeleteSuccessResponse>(`${BANNER_PATH}/${bannerId}`, {
    method: 'delete',
  });
}

export const deleteBannerMutation = mutationOptions({
  mutationFn: deleteBanner,
  retry: false,
});

// ── 버그 제보 ──────────────────────────────────────────────────────────────────────────────

const BUG_REPORT_PATH = '/api/v1/admin/bug-reports';

export async function fetchAdminBugReports(filter: AdminBugReportFilter, cursor?: string) {
  const body = await httpClient<BugReportAdminFindAllSuccessResponse>(BUG_REPORT_PATH, {
    method: 'get',
    searchParams: { status: filter, ...(cursor ? { cursor } : {}) },
  });
  if (!body) throw new Error(`버그 제보 목록 응답이 비어 있습니다: ${BUG_REPORT_PATH}`);
  // 목록은 data, 커서와 전체 개수는 meta에 있다.
  return body;
}

export const adminBugReportsQuery = (filter: AdminBugReportFilter) =>
  infiniteQueryOptions({
    queryKey: adminQueryKeys.bugReports(filter),
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam }) => fetchAdminBugReports(filter, pageParam),
    getNextPageParam: (last) => last.meta.nextCursor ?? undefined,
  });

export async function fetchAdminBugReport(bugReportId: number) {
  const body = await httpClient<BugReportAdminDetailSuccessResponse>(
    `${BUG_REPORT_PATH}/${bugReportId}`,
    { method: 'get' },
  );
  if (!body) throw new Error('버그 제보 상세를 확인하지 못했습니다.');
  return body.data;
}

export const adminBugReportQuery = (bugReportId: number) =>
  queryOptions({
    queryKey: adminQueryKeys.bugReport(bugReportId),
    queryFn: () => fetchAdminBugReport(bugReportId),
  });

export async function updateBugReportStatus({
  bugReportId,
  status,
}: {
  bugReportId: number;
  status: AdminBugReportStatus;
}) {
  const body = await httpClient<BugReportStatusUpdateSuccessResponse>(
    `${BUG_REPORT_PATH}/${bugReportId}/status`,
    { method: 'patch', json: { status } },
  );
  if (!body) throw new Error('버그 제보 상태 변경 결과를 확인하지 못했습니다.');
  return body.data;
}

export const updateBugReportStatusMutation = mutationOptions({
  mutationFn: updateBugReportStatus,
  retry: false,
});

// ── 인터뷰 ──────────────────────────────────────────────────────────────────

export async function fetchAdminInterviews(cursor?: string) {
  const body = await httpClient<InterviewFindAllSuccessResponse>(INTERVIEW_PATH, {
    method: 'get',
    searchParams: cursor ? { cursor } : {},
  });
  if (!body) throw new Error(`관리자 인터뷰 목록 응답이 비어 있습니다: ${INTERVIEW_PATH}`);
  return body;
}

/** 초안을 포함한 전체 인터뷰. 생성 시각 내림차순. */
export const adminInterviewsQuery = infiniteQueryOptions({
  queryKey: adminQueryKeys.interviews,
  initialPageParam: undefined as string | undefined,
  queryFn: ({ pageParam }) => fetchAdminInterviews(pageParam),
  getNextPageParam: (last) =>
    last.meta?.hasNext ? (last.meta.nextCursor ?? undefined) : undefined,
});

/** 응답 봉투에서 인터뷰 원본을 꺼낸다. 등록·조회·수정·발행이 같은 응답을 쓴다. */
function toAdminInterview(body: InterviewAdminResponse | null, action: string) {
  if (!body?.data) throw new Error(`인터뷰 ${action} 결과를 확인하지 못했습니다.`);
  return body.data;
}

export async function fetchAdminInterview(interviewId: number) {
  const body = await httpClient<InterviewAdminResponse>(`${INTERVIEW_PATH}/${interviewId}`, {
    method: 'get',
  });
  return toAdminInterview(body, '조회');
}

/** 이미지 원본 참조(`media://ID`)가 담긴 본문과, 미리보기용 본문을 함께 받는다. */
export const adminInterviewQuery = (interviewId: number) =>
  queryOptions({
    queryKey: adminQueryKeys.interview(interviewId),
    queryFn: () => fetchAdminInterview(interviewId),
  });

/** 초안으로 등록한다. 발행은 따로 한다. */
export async function createInterview(input: InterviewSaveBody) {
  const body = await httpClient<InterviewAdminResponse>(INTERVIEW_PATH, {
    method: 'post',
    json: input,
  });
  return toAdminInterview(body, '등록');
}

export const createInterviewMutation = mutationOptions({
  mutationFn: createInterview,
  retry: false,
});

/** 전체 교체. 보내지 않은 선택 필드는 비워진다. */
export async function updateInterview({
  interviewId,
  body: input,
}: {
  interviewId: number;
  body: InterviewSaveBody;
}) {
  const body = await httpClient<InterviewAdminResponse>(`${INTERVIEW_PATH}/${interviewId}`, {
    method: 'put',
    json: input,
  });
  return toAdminInterview(body, '수정');
}

export const updateInterviewMutation = mutationOptions({
  mutationFn: updateInterview,
  retry: false,
});

export async function deleteInterview(interviewId: number) {
  await httpClient(`${INTERVIEW_PATH}/${interviewId}`, { method: 'delete' });
}

export const deleteInterviewMutation = mutationOptions({
  mutationFn: deleteInterview,
  retry: false,
});

/** 요약·본문·대표 이미지가 모두 있어야 발행된다(`INTERVIEW_PUBLISH_INCOMPLETE`). */
export async function publishInterview(interviewId: number) {
  const body = await httpClient<InterviewAdminResponse>(
    `${INTERVIEW_PATH}/${interviewId}/publish`,
    { method: 'post' },
  );
  return toAdminInterview(body, '발행');
}

export const publishInterviewMutation = mutationOptions({
  mutationFn: publishInterview,
  retry: false,
});

export async function unpublishInterview(interviewId: number) {
  const body = await httpClient<InterviewAdminResponse>(
    `${INTERVIEW_PATH}/${interviewId}/unpublish`,
    { method: 'post' },
  );
  return toAdminInterview(body, '발행 취소');
}

export const unpublishInterviewMutation = mutationOptions({
  mutationFn: unpublishInterview,
  retry: false,
});
