import type { ProfileTab } from '@/types/user';

export const PROFILE_TABS = ['projects', 'feeds'] as const;

export const DEFAULT_PROFILE_TAB: ProfileTab = 'projects';
