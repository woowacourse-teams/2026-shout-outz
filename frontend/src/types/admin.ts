import { ADMIN_TABS } from '@/constants/admin';
import type {
  AdminHomeBannerItem,
  AdminProjectUpdateRequest,
  AdminVerificationItem,
  AdminVerificationListData,
  EventCreateBody,
  HomeBannerUpsertBody,
  NoticeCreateBody,
  ProjectApprovalStatus,
  ProjectDetailData,
  ProjectListItemData,
} from '@/types/api';

export type AdminTab = (typeof ADMIN_TABS)[number];

// 사전에 정의한 값이 아닐 경우 undefined로 덮기 위한 용도의 타입 가드
export const isAdminTab = (value: unknown): value is AdminTab =>
  ADMIN_TABS.some((tab) => tab === value);

/** 인증 신청 목록. `GET /api/v1/admin/verification-requests` */
export type AdminVerificationList = AdminVerificationListData;
export type AdminVerification = AdminVerificationItem;
export type AdminVerificationStatus = AdminVerification['status'];

/** 관리자 홈 배너 한 건. `GET /api/v1/admin/home/banners` */
export type AdminHomeBanner = AdminHomeBannerItem;
export type { HomeBannerUpsertBody };

export type { NoticeCreateBody, EventCreateBody };

/**
 * AdminProjectFindAllResponse.Item.java.
 * 관리자 심사 목록은 일반 목록과 달리 숫자 ID를 유지한다.
 */
export type AdminProject = Pick<
  ProjectListItemData,
  'slug' | 'title' | 'tagline' | 'cohort' | 'members'
> & {
  id: number;
  approvalStatus: ProjectApprovalStatus;
  rejectReason: string | null;
};
export type AdminProjectStatus = AdminProject['approvalStatus'];

/** AdminProjectDetailResponse.java에서 심사·수정 화면이 사용하는 필드. */
export type AdminProjectDetail = Pick<
  ProjectDetailData,
  | 'slug'
  | 'title'
  | 'teamName'
  | 'tagline'
  | 'cohort'
  | 'thumbnailImageId'
  | 'imageUrl'
  | 'githubRepositoryUrl'
  | 'deploymentUrl'
  | 'descriptionMd'
  | 'serviceStatus'
  | 'techTags'
  | 'members'
> & { id: number; approvalStatus: ProjectApprovalStatus };

/**
 * 관리자 프로젝트 수정 요청. `PATCH /api/v1/admin/projects/{projectId}/migration`
 *
 * 보낸 필드만 바뀌고, 작성자·승인 상태 전이 검사를 거치지 않는다. 팀원은 이 요청으로 바꿀 수 없다.
 */
export type AdminProjectUpdateBody = AdminProjectUpdateRequest;

/** AdminProjectApproveResponse.java와 AdminProjectRejectResponse.java. */
export interface AdminProjectDecision {
  projectId: number;
  approvalStatus: ProjectApprovalStatus;
  decidedBy: { userId: number; handle: string };
  decidedAt: string;
}

export type AdminProjectRejection = AdminProjectDecision & { reason: string };
