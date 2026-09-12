import { DARK_MODE_QUERY, THEME_CHANGE_EVENT, THEME_STORAGE_KEY } from '../utils/theme';

function initializeTheme(storageKey: string, darkModeQuery: string, themeChangeEvent: string) {
  const getStoredTheme = () => {
    try {
      return localStorage.getItem(storageKey);
    } catch {
      return null;
    }
  };

  const applyTheme = () => {
    const value = getStoredTheme();
    const theme = value === 'light' || value === 'dark' || value === 'system' ? value : 'system';
    const isDark = theme === 'dark' || (theme === 'system' && matchMedia(darkModeQuery).matches);

    if (isDark) {
      document.documentElement.dataset.theme = 'dark';
    } else {
      document.documentElement.removeAttribute('data-theme');
    }
  };

  applyTheme();

  matchMedia(darkModeQuery).addEventListener('change', applyTheme);
  window.addEventListener('storage', (event) => {
    if (event.key === null || event.key === storageKey) {
      applyTheme();
    }
  });
  window.addEventListener(themeChangeEvent, applyTheme);
}

const themeScript = `(${initializeTheme.toString()})(${JSON.stringify(THEME_STORAGE_KEY)}, ${JSON.stringify(DARK_MODE_QUERY)}, ${JSON.stringify(THEME_CHANGE_EVENT)})`;

export function ThemeScript() {
  return <script dangerouslySetInnerHTML={{ __html: themeScript }} />;
}
