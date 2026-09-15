export function getApiUrl(path: string): URL {
  const origin = typeof window === 'undefined' ? process.env.API_ORIGIN : window.location.origin;
  if (!origin) {
    throw new Error('SSG 빌드에는 API_ORIGIN 환경 변수가 필요합니다.');
  }
  return new URL(path, origin);
}
