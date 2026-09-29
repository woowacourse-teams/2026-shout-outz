import { infiniteQueryOptions, mutationOptions, queryOptions } from '@tanstack/react-query';
import { httpClient, type ApiSuccessBody } from '@/utils/client';
import type {
  AdminVerificationRequestApproveSuccessResponse,
  AdminVerificationRequestFindAllSuccessResponse,
  AdminVerificationRequestRejectSuccessResponse,
  EventCreateSuccessResponse,
  HomeBannerAdminFindAllSuccessResponse,
  HomeBannerAdminSaveSuccessResponse,
  HomeBannerAdminUpdateSuccessResponse,
  HomeBannerDeleteSuccessResponse,
  NoticeCreateSuccessResponse,
} from '@/api/generated/schema';
import type {
  AdminProject,
  AdminProjectStatus,
  AdminVerificationStatus,
  EventCreateBody,
  HomeBannerUpsertBody,
  NoticeCreateBody,
} from '@/types/admin';

const VERIFICATION_PATH = '/api/v1/admin/verification-requests';
const BANNER_PATH = '/api/v1/admin/home/banners';
// TODO 관리자 프로젝트 심사 API가 명세에 없다. 인증 신청 심사와 같은 모양으로 가정한 주소다.
const PROJECT_PATH = '/api/v1/admin/projects';

export const adminQueryKeys = {
  verifications: (status: AdminVerificationStatus) =>
    ['admin', 'verification-requests', status] as const,
  projects: (status: AdminProjectStatus) => ['admin', 'projects', status] as const,
  banners: ['admin', 'home-banners'] as const,
};

// ── 크루 인증 신청 ───────────────────────────────────────────────────────────

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

// ── 프로젝트 심사 (명세 없음, 가정) ──────────────────────────────────────────

interface AdminProjectPage {
  items: AdminProject[];
  nextCursor?: string | null;
}

export async function fetchAdminProjects(status: AdminProjectStatus, cursor?: string) {
  const body = await httpClient<ApiSuccessBody<AdminProjectPage>>(PROJECT_PATH, {
    method: 'get',
    searchParams: { status, ...(cursor ? { cursor } : {}) },
  });
  if (!body) throw new Error(`프로젝트 심사 목록 응답이 비어 있습니다: ${PROJECT_PATH}`);
  return body.data;
}

export const adminProjectsQuery = (status: AdminProjectStatus) =>
  infiniteQueryOptions({
    queryKey: adminQueryKeys.projects(status),
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam }) => fetchAdminProjects(status, pageParam),
    getNextPageParam: (last) => last.nextCursor ?? undefined,
  });

export async function approveProject(projectId: number) {
  await httpClient(`${PROJECT_PATH}/${projectId}/approve`, { method: 'post' });
}

export const approveProjectMutation = mutationOptions({
  mutationFn: approveProject,
  retry: false,
});

export async function rejectProject({ projectId, reason }: { projectId: number; reason: string }) {
  await httpClient(`${PROJECT_PATH}/${projectId}/reject`, { method: 'post', json: { reason } });
}

export const rejectProjectMutation = mutationOptions({
  mutationFn: rejectProject,
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
