export type Theme = 'light' | 'dark' | 'system';
export type ResolvedTheme = Exclude<Theme, 'system'>;

export const THEME_STORAGE_KEY = 'shout-outz-theme';
export const DARK_MODE_QUERY = '(prefers-color-scheme: dark)';
export const THEME_CHANGE_EVENT = 'themechange';

function isTheme(value: string | null): value is Theme {
  return value === 'light' || value === 'dark' || value === 'system';
}

export function getTheme(): Theme {
  const theme = localStorage.getItem(THEME_STORAGE_KEY);

  return isTheme(theme) ? theme : 'system';
}

export function getResolvedTheme(): ResolvedTheme {
  const theme = getTheme();

  if (theme !== 'system') {
    return theme;
  }

  return matchMedia(DARK_MODE_QUERY).matches ? 'dark' : 'light';
}

export function setTheme(theme: Theme) {
  localStorage.setItem(THEME_STORAGE_KEY, theme);
  window.dispatchEvent(new Event(THEME_CHANGE_EVENT));
}

export function subscribeTheme(listener: () => void) {
  const mediaQuery = matchMedia(DARK_MODE_QUERY);

  const handleSystemThemeChange = () => {
    if (getTheme() !== 'system') {
      return;
    }

    listener();
  };

  const handleStorageChange = (event: StorageEvent) => {
    if (event.key !== null && event.key !== THEME_STORAGE_KEY) {
      return;
    }

    listener();
  };

  const handleThemeChange = () => {
    listener();
  };

  mediaQuery.addEventListener('change', handleSystemThemeChange);
  window.addEventListener('storage', handleStorageChange);
  window.addEventListener(THEME_CHANGE_EVENT, handleThemeChange);

  return () => {
    mediaQuery.removeEventListener('change', handleSystemThemeChange);
    window.removeEventListener('storage', handleStorageChange);
    window.removeEventListener(THEME_CHANGE_EVENT, handleThemeChange);
  };
}
