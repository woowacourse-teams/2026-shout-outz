const GITHUB_AUTHORIZATION_PATH = '/oauth2/authorization/github';

export function normalizeApiOrigin(origin?: string) {
  const value = origin?.trim();
  if (!value) return undefined;

  const withProtocol = /^https?:\/\//i.test(value) ? value : `http://${value}`;
  return withProtocol.replace(/\/+$/, '');
}

export function getGithubLoginUrl() {
  const origin =
    getApiOrigin() || (typeof window === 'undefined' ? undefined : window.location.origin);

  if (!origin) return GITHUB_AUTHORIZATION_PATH;
  return new URL(GITHUB_AUTHORIZATION_PATH, origin).toString();
}

export function getApiOrigin() {
  return normalizeApiOrigin(
    typeof process.env.API_ORIGIN === 'undefined' ? undefined : process.env.API_ORIGIN,
  );
}
