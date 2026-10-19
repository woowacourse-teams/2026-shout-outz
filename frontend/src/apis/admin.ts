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
  MagazineAdminResponse,
  MagazineFindAllSuccessResponse,
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
  MagazineSaveBody,
  NoticeCreateBody,
} from '@/types/admin';
import type { ProjectListMeta } from '@/types/project';

const VERIFICATION_PATH = '/api/v1/admin/verification-requests';
const BANNER_PATH = '/api/v1/admin/home/banners';
const PROJECT_PATH = '/api/v1/admin/projects';
const MAGAZINE_PATH = '/api/v1/admin/magazines';

export const adminQueryKeys = {
  verifications: (status: AdminVerificationStatus) =>
    ['admin', 'verification-requests', status] as const,
  projects: (status: AdminProjectStatus) => ['admin', 'projects', status] as const,
  banners: ['admin', 'home-banners'] as const,
  bugReports: (filter: AdminBugReportFilter) => ['admin', 'bug-reports', filter] as const,
  bugReport: (bugReportId: number) => ['admin', 'bug-report', bugReportId] as const,
  magazines: ['admin', 'magazines'] as const,
  magazine: (magazineId: number) => ['admin', 'magazine', magazineId] as const,
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

// ── 매거진 ──────────────────────────────────────────────────────────────────

export async function fetchAdminMagazines(cursor?: string) {
  const body = await httpClient<MagazineFindAllSuccessResponse>(MAGAZINE_PATH, {
    method: 'get',
    searchParams: cursor ? { cursor } : {},
  });
  if (!body) throw new Error(`관리자 매거진 목록 응답이 비어 있습니다: ${MAGAZINE_PATH}`);
  return body;
}

/** 초안을 포함한 전체 매거진. 생성 시각 내림차순. */
export const adminMagazinesQuery = infiniteQueryOptions({
  queryKey: adminQueryKeys.magazines,
  initialPageParam: undefined as string | undefined,
  queryFn: ({ pageParam }) => fetchAdminMagazines(pageParam),
  getNextPageParam: (last) =>
    last.meta?.hasNext ? (last.meta.nextCursor ?? undefined) : undefined,
});

/** 응답 봉투에서 매거진 원본을 꺼낸다. 등록·조회·수정·발행이 같은 응답을 쓴다. */
function toAdminMagazine(body: MagazineAdminResponse | null, action: string) {
  if (!body?.data) throw new Error(`매거진 ${action} 결과를 확인하지 못했습니다.`);
  return body.data;
}

export async function fetchAdminMagazine(magazineId: number) {
  const body = await httpClient<MagazineAdminResponse>(`${MAGAZINE_PATH}/${magazineId}`, {
    method: 'get',
  });
  return toAdminMagazine(body, '조회');
}

/** 이미지 원본 참조(`media://ID`)가 담긴 본문과, 미리보기용 본문을 함께 받는다. */
export const adminMagazineQuery = (magazineId: number) =>
  queryOptions({
    queryKey: adminQueryKeys.magazine(magazineId),
    queryFn: () => fetchAdminMagazine(magazineId),
  });

/** 초안으로 등록한다. 발행은 따로 한다. */
export async function createMagazine(input: MagazineSaveBody) {
  const body = await httpClient<MagazineAdminResponse>(MAGAZINE_PATH, {
    method: 'post',
    json: input,
  });
  return toAdminMagazine(body, '등록');
}

export const createMagazineMutation = mutationOptions({
  mutationFn: createMagazine,
  retry: false,
});

/** 전체 교체. 보내지 않은 선택 필드는 비워진다. */
export async function updateMagazine({
  magazineId,
  body: input,
}: {
  magazineId: number;
  body: MagazineSaveBody;
}) {
  const body = await httpClient<MagazineAdminResponse>(`${MAGAZINE_PATH}/${magazineId}`, {
    method: 'put',
    json: input,
  });
  return toAdminMagazine(body, '수정');
}

export const updateMagazineMutation = mutationOptions({
  mutationFn: updateMagazine,
  retry: false,
});

export async function deleteMagazine(magazineId: number) {
  await httpClient(`${MAGAZINE_PATH}/${magazineId}`, { method: 'delete' });
}

export const deleteMagazineMutation = mutationOptions({
  mutationFn: deleteMagazine,
  retry: false,
});

/** 요약·본문·대표 이미지·인터뷰이 소개가 모두 있어야 발행된다(`MAGAZINE_PUBLISH_INCOMPLETE`). */
export async function publishMagazine(magazineId: number) {
  const body = await httpClient<MagazineAdminResponse>(`${MAGAZINE_PATH}/${magazineId}/publish`, {
    method: 'post',
  });
  return toAdminMagazine(body, '발행');
}

export const publishMagazineMutation = mutationOptions({
  mutationFn: publishMagazine,
  retry: false,
});

export async function unpublishMagazine(magazineId: number) {
  const body = await httpClient<MagazineAdminResponse>(`${MAGAZINE_PATH}/${magazineId}/unpublish`, {
    method: 'post',
  });
  return toAdminMagazine(body, '발행 취소');
}

export const unpublishMagazineMutation = mutationOptions({
  mutationFn: unpublishMagazine,
  retry: false,
});
