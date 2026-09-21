const PATTERNS: [RegExp, string][] = [
  [/^\/users\/[^/]+$/, '/users/:handle'],
  [/^\/feeds\/\d+$/, '/feeds/:feedId'],
  [/^\/feeds\/\d+\/edit$/, '/feeds/:feedId/edit'],
  [/^\/projects\/\d+$/, '/projects/:projectId'],
  [/^\/news\/\d+$/, '/news/:newsId'],
];

const KEPT_SEARCH_PARAMS = ['tab', 'sort', 'type'];

export function toPathPattern(pathWithSearch: string): string {
  const [rawPath = '', rawSearch = ''] = pathWithSearch.split('?');
  const path = rawPath.length > 1 ? rawPath.replace(/\/+$/, '') : rawPath;

  const matched = PATTERNS.find(([pattern]) => pattern.test(path));
  const patternedPath = matched ? matched[1] : path;

  const kept = [...new URLSearchParams(rawSearch)].filter(([key]) =>
    KEPT_SEARCH_PARAMS.includes(key),
  );
  if (kept.length === 0) return patternedPath;

  return `${patternedPath}?${kept.map(([key, value]) => `${key}=${value}`).join('&')}`;
}
