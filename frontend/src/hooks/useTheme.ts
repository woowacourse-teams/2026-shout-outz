import {
  getResolvedTheme,
  getTheme,
  setTheme,
  subscribeTheme,
  type ResolvedTheme,
  type Theme,
} from '@/utils/theme';
import { useSyncExternalStore } from 'react';

const getThemeServerSnapshot = (): Theme | undefined => {
  if (typeof document === 'undefined') {
    return undefined;
  }

  return getTheme();
};

const getResolvedServerSnapshot = (): ResolvedTheme | undefined => {
  if (typeof document === 'undefined') {
    return undefined;
  }

  return getResolvedTheme();
};

export function useTheme() {
  const theme = useSyncExternalStore(subscribeTheme, getTheme, getThemeServerSnapshot);
  const resolvedTheme = useSyncExternalStore(
    subscribeTheme,
    getResolvedTheme,
    getResolvedServerSnapshot,
  );

  return {
    theme,
    resolvedTheme,
    setTheme,
  };
}
