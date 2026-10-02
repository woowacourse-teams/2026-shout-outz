import { Tab } from '@/components/Tab';
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

export function ProfileTabs({ value, projectCount, feedCount, onChange }: ProfileTabsProps) {
  const tabs: { value: ProfileTab; label: string }[] = [
    { value: 'projects', label: `프로젝트 (${projectCount})` },
    { value: 'feeds', label: `피드 (${feedCount})` },
  ];

  return (
    <Tab
      variant="chip"
      size="sm"
      value={value}
      onChange={(next) => onChange(next as ProfileTab)}
      aria-label="프로필 탭"
    >
      {tabs.map((tab) => (
        <Tab.Item key={tab.value} value={tab.value}>
          {tab.label}
        </Tab.Item>
      ))}
    </Tab>
  );
}
