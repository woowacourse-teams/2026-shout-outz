import type { AdminTab } from '@/types/admin';

export const ADMIN_TABS = ['crews', 'projects', 'news', 'banners'] as const;

export const DEFAULT_ADMIN_TAB: AdminTab = 'crews';

export const ADMIN_TAB_LABELS: Record<AdminTab, string> = {
  crews: '크루 승인',
  projects: '프로젝트 승인',
  news: '소식 등록',
  banners: '홈 배너',
};

export const REVIEW_STATUS_LABELS = {
  PENDING: '대기',
  APPROVED: '승인',
  REJECTED: '반려',
} as const;
