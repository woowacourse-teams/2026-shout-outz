import { getApiOrigin } from '@/utils/auth';

export function getApiUrl(path: string): URL {
  const origin =
    getApiOrigin() || (typeof window === 'undefined' ? undefined : window.location.origin);
  if (!origin) {
    throw new Error('SSG 빌드에는 API_ORIGIN 환경 변수가 필요합니다.');
  }
  return new URL(path, origin);
}
