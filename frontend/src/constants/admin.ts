import type { AdminTab } from '@/types/admin';

export const ADMIN_TABS = [
  'crews',
  'projects',
  'project-edit',
  'news',
  'job-postings',
  'categories',
  'banners',
  'magazines',
  'bug-reports',
  'resource-reports',
] as const;

export const DEFAULT_ADMIN_TAB: AdminTab = 'crews';

export const ADMIN_TAB_LABELS: Record<AdminTab, string> = {
  crews: '우아한테크코스 소속 인증',
  projects: '프로젝트 승인',
  'project-edit': '프로젝트 수정',
  news: '소식',
  'job-postings': '채용공고',
  categories: '카테고리',
  banners: '홈 배너',
  magazines: '매거진',
  'bug-reports': '버그 제보',
  'resource-reports': '자료 제보',
};

export const BUG_REPORT_STATUS_LABELS = {
  OPEN: '접수',
  COMPLETED: '처리 완료',
} as const;

export const BUG_REPORT_FILTER_LABELS = {
  ...BUG_REPORT_STATUS_LABELS,
  ALL: '전체',
} as const;

export const RESOURCE_REPORT_STATUS_LABELS = {
  OPEN: '접수',
  ACCEPTED: '반영',
  REJECTED: '반영 안 함',
} as const;

export const RESOURCE_REPORT_FILTER_LABELS = {
  ...RESOURCE_REPORT_STATUS_LABELS,
  ALL: '전체',
} as const;

export const RESOURCE_REPORT_KIND_LABELS = {
  REMOVE: '필요 없어요',
  ADD: '빠진 자료',
} as const;

export const REVIEW_STATUS_LABELS = {
  PENDING: '대기',
  APPROVED: '승인',
  REJECTED: '반려',
} as const;
