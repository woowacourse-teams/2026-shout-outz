import type { JobRole } from '@/apis/job-posting';

export const JOB_ROLE_LABELS: Record<JobRole, string> = {
  BACKEND: 'Backend',
  FRONTEND: 'Frontend',
  ANDROID: 'Android',
  IOS: 'iOS',
  DEVOPS: 'DevOps',
  DATA: 'Data',
  AI: 'AI',
  ETC: '기타',
};

const MS_PER_DAY = 24 * 60 * 60 * 1000;

/** YYYY-MM-DD를 시간대 영향 없이 그날 0시의 로컬 날짜로 읽는다. */
const parseDate = (value: string) => {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year!, month! - 1, day!);
};

/** 카드 배지. 마감일이 없으면 상시, 있으면 남은 날을 D-n(당일은 D-day)으로 보여 준다. */
export function deadlineBadge(deadline: string | null, today = new Date()) {
  if (!deadline) return '상시';
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  const days = Math.round((parseDate(deadline).getTime() - start.getTime()) / MS_PER_DAY);
  return days <= 0 ? 'D-day' : `D-${days}`;
}

/** 상세 제목 아래 문구. 마감일이 있으면 "2026년 12월 31일 마감"이다. */
export function deadlineText(deadline: string | null) {
  if (!deadline) return '상시 채용';
  const date = parseDate(deadline);
  return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일 마감`;
}
