import { kyInstance } from '@/utils/http';
import { type Options } from 'ky';

export type HttpMethod = 'get' | 'post' | 'put' | 'patch' | 'delete';

interface ApiSuccessBody<T, K = unknown> {
  status: 'success';
  data: T;
  meta?: K;
}

const isApiSuccessBody = (value: unknown): value is ApiSuccessBody<unknown> =>
  typeof value === 'object' &&
  value !== null &&
  (value as ApiSuccessBody<unknown, unknown>).status === 'success' &&
  'data' in value;

export const httpClient = async <T>(
  url: string,
  method: HttpMethod = 'get',
  options: Omit<Options, 'method'> = {},
): Promise<T | null> => {
  const response = await kyInstance(url, { ...options, method });

  // TODO 명세상 DELETE 응답이 200과 204로 나뉘어 있어 두 경우를 모두 받는다.
  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return null;
  }

  const body: unknown = await response.json();

  if (!isApiSuccessBody(body)) {
    throw new Error(`응답이 공통 규격을 따르지 않습니다: ${method.toUpperCase()} ${url}`);
  }

  return body as T;
};
