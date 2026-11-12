import { infiniteQueryOptions, mutationOptions, queryOptions } from '@tanstack/react-query';
import { httpClient, type ApiSuccessBody } from '@/utils/client';
import type {
  AdminJobPostingDetailSuccessResponse,
  AdminJobPostingExtractRequest,
  AdminJobPostingExtractSuccessResponse,
  AdminJobPostingFindAllSuccessResponse,
  AdminJobPostingReanalysisRequest,
  AdminJobPostingReanalysisSuccessResponse,
  AdminJobPostingSaveRequest,
  AdminJobPostingSaveSuccessResponse,
  AdminJobPostingUpdateSuccessResponse,
  AdminJobPostingVisibilityUpdateRequest,
  AdminJobPostingVisibilityUpdateSuccessResponse,
  AdminProjectHistorySuccessResponse,
  AdminVerificationRequestHistorySuccessResponse,
  CategorySaveRequest,
  CategorySaveSuccessResponse,
  CategoryUpdateRequest,
  CategoryUpdateSuccessResponse,
  NewsDeleteSuccessResponse,
  NewsUpdateRequest,
  NewsUpdateSuccessResponse,
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
  ResourceReportAdminFindAllSuccessResponse,
  ResourceReportStatusUpdateSuccessResponse,
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
  AdminResourceReportFilter,
  AdminResourceReportKind,
  AdminResourceReportStatus,
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
  resourceReports: (filter: AdminResourceReportFilter, kind: AdminResourceReportKind | null) =>
    ['admin', 'resource-reports', filter, kind] as const,
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

// ── 자료 제보 ──────────────────────────────────────────────────────────────────────────────

const RESOURCE_REPORT_PATH = '/api/v1/admin/resource-reports';

export async function fetchAdminResourceReports(
  filter: AdminResourceReportFilter,
  kind: AdminResourceReportKind | null,
  cursor?: string,
) {
  const body = await httpClient<ResourceReportAdminFindAllSuccessResponse>(RESOURCE_REPORT_PATH, {
    method: 'get',
    searchParams: { status: filter, ...(kind ? { kind } : {}), ...(cursor ? { cursor } : {}) },
  });
  if (!body) throw new Error(`자료 제보 목록 응답이 비어 있습니다: ${RESOURCE_REPORT_PATH}`);
  return body;
}

export const adminResourceReportsQuery = (
  filter: AdminResourceReportFilter,
  kind: AdminResourceReportKind | null,
) =>
  infiniteQueryOptions({
    queryKey: adminQueryKeys.resourceReports(filter, kind),
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam }) => fetchAdminResourceReports(filter, kind, pageParam),
    getNextPageParam: (last) => last.meta.nextCursor ?? undefined,
  });

/** 반영(ACCEPTED)하면 서버가 필요 없어요 자료를 검색과 추천에서 뺀다. */
export async function updateResourceReportStatus({
  reportId,
  status,
}: {
  reportId: number;
  status: AdminResourceReportStatus;
}) {
  const body = await httpClient<ResourceReportStatusUpdateSuccessResponse>(
    `${RESOURCE_REPORT_PATH}/${reportId}/status`,
    { method: 'patch', json: { status } },
  );
  if (!body) throw new Error('자료 제보 처리 결과를 확인하지 못했습니다.');
  return body.data;
}

export const updateResourceReportStatusMutation = mutationOptions({
  mutationFn: updateResourceReportStatus,
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

// ── 심사 이력 ─────────────────────────────────────────────────────────────────────────────

export async function fetchAdminProjectHistory(projectId: number) {
  const path = `${PROJECT_PATH}/${projectId}/history`;
  const body = await httpClient<AdminProjectHistorySuccessResponse>(path, { method: 'get' });
  if (!body) throw new Error(`프로젝트 심사 이력 응답이 비어 있습니다: ${path}`);
  return body.data.items;
}

export async function fetchAdminVerificationHistory(requestId: number) {
  const path = `${VERIFICATION_PATH}/${requestId}/history`;
  const body = await httpClient<AdminVerificationRequestHistorySuccessResponse>(path, {
    method: 'get',
  });
  if (!body) throw new Error(`인증 심사 이력 응답이 비어 있습니다: ${path}`);
  return body.data.items;
}

// ── 채용공고 ──────────────────────────────────────────────────────────────────────────────

const JOB_POSTING_PATH = '/api/v1/admin/job-postings';

export async function fetchAdminJobPostings(cursor?: string) {
  const body = await httpClient<AdminJobPostingFindAllSuccessResponse>(JOB_POSTING_PATH, {
    method: 'get',
    searchParams: cursor ? { cursor } : {},
  });
  if (!body) throw new Error(`관리자 채용공고 목록 응답이 비어 있습니다: ${JOB_POSTING_PATH}`);
  return body;
}

/** 숨긴 공고까지 포함한 전체 채용공고. */
export const adminJobPostingsQuery = infiniteQueryOptions({
  queryKey: ['admin', 'job-postings'],
  initialPageParam: undefined as string | undefined,
  queryFn: ({ pageParam }) => fetchAdminJobPostings(pageParam),
  getNextPageParam: (last) => (last.meta.hasNext ? (last.meta.nextCursor ?? undefined) : undefined),
});

export async function fetchAdminJobPosting(jobPostingId: number) {
  const path = `${JOB_POSTING_PATH}/${jobPostingId}`;
  const body = await httpClient<AdminJobPostingDetailSuccessResponse>(path, { method: 'get' });
  if (!body) throw new Error(`관리자 채용공고 상세 응답이 비어 있습니다: ${path}`);
  return body.data as AdminJobPostingDetail;
}

export const adminJobPostingQuery = (jobPostingId: number) =>
  queryOptions({
    queryKey: ['admin', 'job-posting', jobPostingId],
    queryFn: () => fetchAdminJobPosting(jobPostingId),
  });

/**
 * 추출은 서버가 LLM을 불러 보통 10~20초 걸린다(서버 LLM 제한 45초).
 * 공용 설정(timeout 10초, totalTimeout 30초)이면 화면이 먼저 끊으므로 이 요청만 60초로 늘린다.
 * totalTimeout은 재시도를 포함한 전체 상한이라 timeout만 늘리면 30초에서 끊겨 함께 늘린다.
 */
export const JOB_POSTING_EXTRACT_TIMEOUT_MS = 60_000;

/** 공고 본문을 붙여 넣으면 서버가 회사·직군·요구사항을 뽑아 등록 폼 초안을 준다. 저장하지는 않는다. */
export async function extractJobPosting(input: AdminJobPostingExtractRequest) {
  const body = await httpClient<AdminJobPostingExtractSuccessResponse>(
    `${JOB_POSTING_PATH}/extractions`,
    {
      method: 'post',
      json: input,
      timeout: JOB_POSTING_EXTRACT_TIMEOUT_MS,
      totalTimeout: JOB_POSTING_EXTRACT_TIMEOUT_MS,
    },
  );
  if (!body) throw new Error('채용공고 추출 결과를 확인하지 못했습니다.');
  return body.data;
}

export const extractJobPostingMutation = mutationOptions({
  mutationFn: extractJobPosting,
  retry: false,
});

export async function createJobPosting(input: AdminJobPostingCreateBody) {
  const body = await httpClient<AdminJobPostingSaveSuccessResponse>(JOB_POSTING_PATH, {
    method: 'post',
    json: input,
  });
  if (!body) throw new Error('채용공고 등록 결과를 확인하지 못했습니다.');
  return body.data;
}

export const createJobPostingMutation = mutationOptions({
  mutationFn: createJobPosting,
  retry: false,
});

/**
 * 채용공고 등록 요청. 생성된 schema.ts에 아직 회사 로고(logoImageId)가 없어 여기서 더한다.
 * logoImageId는 JOB_POSTING_COMPANY_LOGO로 올린 이미지 ID다.
 */
export type AdminJobPostingCreateBody = AdminJobPostingSaveRequest & {
  logoImageId?: number | null;
};

/**
 * 채용공고 수정 요청. 전체 교체라 logoImageId를 다시 보내야 로고가 남는다.
 *
 * - 본문을 안 바꿀 때: content와 requirements를 빼고 보낸다. 공고 정보만 바뀌고 역량·분석 상태는 그대로다.
 * - 본문을 바꿀 때: content와 requirements를 같이 보낸다. 본문이 실제로 바뀌었으면 다시 분석한다.
 * - content만 보내고 requirements를 비우면 400(requirementsPresentWithContent)이다.
 */
export type AdminJobPostingUpdateBody = Omit<
  AdminJobPostingCreateBody,
  'content' | 'requirements'
> &
  (
    | { content?: undefined; requirements?: undefined }
    | Pick<AdminJobPostingSaveRequest, 'content' | 'requirements'>
  );

/** 생성된 schema.ts에 아직 없는 회사 로고 필드를 더한 관리자 채용공고 상세. */
export type AdminJobPostingDetail = AdminJobPostingDetailSuccessResponse['data'] & {
  logoImageId?: number | null;
  logoUrl?: string | null;
};

export async function updateJobPosting({
  jobPostingId,
  input,
}: {
  jobPostingId: number;
  input: AdminJobPostingUpdateBody;
}) {
  const body = await httpClient<AdminJobPostingUpdateSuccessResponse>(
    `${JOB_POSTING_PATH}/${jobPostingId}`,
    { method: 'put', json: input },
  );
  if (!body) throw new Error('채용공고 수정 결과를 확인하지 못했습니다.');
  return body.data;
}

export const updateJobPostingMutation = mutationOptions({
  mutationFn: updateJobPosting,
  retry: false,
});

export async function updateJobPostingVisibility({
  jobPostingId,
  hidden,
}: {
  jobPostingId: number;
} & AdminJobPostingVisibilityUpdateRequest) {
  const body = await httpClient<AdminJobPostingVisibilityUpdateSuccessResponse>(
    `${JOB_POSTING_PATH}/${jobPostingId}/visibility`,
    { method: 'patch', json: { hidden } },
  );
  if (!body) throw new Error('채용공고 공개 상태 변경 결과를 확인하지 못했습니다.');
  return body.data;
}

export const updateJobPostingVisibilityMutation = mutationOptions({
  mutationFn: updateJobPostingVisibility,
  retry: false,
});

/** 요구사항별 추천 자료를 다시 찾는다. FAILED는 실패한 요구사항만, ALL은 전부. */
export async function reanalyzeJobPosting({
  jobPostingId,
  scope,
}: {
  jobPostingId: number;
} & AdminJobPostingReanalysisRequest) {
  const body = await httpClient<AdminJobPostingReanalysisSuccessResponse>(
    `${JOB_POSTING_PATH}/${jobPostingId}/reanalysis`,
    { method: 'post', json: { scope } },
  );
  if (!body) throw new Error('채용공고 재분석 결과를 확인하지 못했습니다.');
  return body.data;
}

export const reanalyzeJobPostingMutation = mutationOptions({
  mutationFn: reanalyzeJobPosting,
  retry: false,
});

export async function deleteJobPosting(jobPostingId: number) {
  // 204 No Content라 본문이 없다.
  await httpClient<null>(`${JOB_POSTING_PATH}/${jobPostingId}`, { method: 'delete' });
}

export const deleteJobPostingMutation = mutationOptions({
  mutationFn: deleteJobPosting,
  retry: false,
});

// ── 소식 수정·삭제 ────────────────────────────────────────────────────────────────────────

export async function updateNews({ newsId, input }: { newsId: number; input: NewsUpdateRequest }) {
  const body = await httpClient<NewsUpdateSuccessResponse>(`/api/v1/news/${newsId}`, {
    method: 'put',
    json: input,
  });
  if (!body) throw new Error('소식 수정 결과를 확인하지 못했습니다.');
  return body.data;
}

export const updateNewsMutation = mutationOptions({ mutationFn: updateNews, retry: false });

export async function deleteNews(newsId: number) {
  await httpClient<NewsDeleteSuccessResponse>(`/api/v1/news/${newsId}`, { method: 'delete' });
}

export const deleteNewsMutation = mutationOptions({ mutationFn: deleteNews, retry: false });

// ── 카테고리 ──────────────────────────────────────────────────────────────────────────────

const CATEGORY_PATH = '/api/v1/categories';

export async function createCategory(input: CategorySaveRequest) {
  const body = await httpClient<CategorySaveSuccessResponse>(CATEGORY_PATH, {
    method: 'post',
    json: input,
  });
  if (!body) throw new Error('카테고리 추가 결과를 확인하지 못했습니다.');
  return body.data;
}

export const createCategoryMutation = mutationOptions({ mutationFn: createCategory, retry: false });

export async function updateCategory({
  categoryId,
  input,
}: {
  categoryId: number;
  input: CategoryUpdateRequest;
}) {
  const body = await httpClient<CategoryUpdateSuccessResponse>(`${CATEGORY_PATH}/${categoryId}`, {
    method: 'put',
    json: input,
  });
  if (!body) throw new Error('카테고리 수정 결과를 확인하지 못했습니다.');
  return body.data;
}

export const updateCategoryMutation = mutationOptions({ mutationFn: updateCategory, retry: false });

export async function deleteCategory(categoryId: number) {
  await httpClient<null>(`${CATEGORY_PATH}/${categoryId}`, { method: 'delete' });
}

export const deleteCategoryMutation = mutationOptions({ mutationFn: deleteCategory, retry: false });
