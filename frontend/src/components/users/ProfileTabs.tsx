import { type ProfileTab } from '@/types/user';

/**
 * 프로필 탭 바. "프로젝트 (2)", "피드 (18)"을 보여준다.

 */
export interface ProfileTabsProps {
  value: ProfileTab;
  projectCount: number;
  feedCount: number;
  onChange: (next: ProfileTab) => void;
}

export function ProfileTabs(props: ProfileTabsProps) {
  return null;
}
