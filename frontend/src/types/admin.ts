import { ADMIN_TABS } from '@/constants/admin';
import type {
  AdminHomeBannerItem,
  AdminVerificationItem,
  AdminVerificationListData,
  EventCreateBody,
  HomeBannerUpsertBody,
  NoticeCreateBody,
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
 * 승인 대기 프로젝트 한 건.
 *
 * TODO 관리자 프로젝트 심사 API가 명세에 없다. 목록 항목에 상세의 승인 상태를 붙인 모양으로 가정했다.
 * 명세가 올라오면 생성 타입에서 파생하도록 바꾼다.
 */
export type AdminProject = ProjectListItemData &
  Pick<ProjectDetailData, 'approvalStatus' | 'rejectReason'>;
export type AdminProjectStatus = AdminProject['approvalStatus'];
