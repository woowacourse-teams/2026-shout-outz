import type { AdminTab } from '@/types/admin';

export const ADMIN_TABS = ['crews', 'projects', 'project-edit', 'news', 'banners'] as const;

export const DEFAULT_ADMIN_TAB: AdminTab = 'crews';

export const ADMIN_TAB_LABELS: Record<AdminTab, string> = {
  crews: '우아한테크코스 소속 인증',
  projects: '프로젝트 승인',
  'project-edit': '프로젝트 수정',
  news: '소식 등록',
  banners: '홈 배너',
};

export const REVIEW_STATUS_LABELS = {
  PENDING: '대기',
  APPROVED: '승인',
  REJECTED: '반려',
} as const;
