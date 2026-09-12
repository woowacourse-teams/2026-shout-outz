import { HttpError, isApiErrorBody, type ApiErrorBody } from '@/utils/error';
import { kyInstance } from '@/utils/http';
import { isHTTPError, type Options } from 'ky';

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

const readErrorBody = async (response: Response): Promise<ApiErrorBody | null> => {
  try {
    const parsed: unknown = await response.clone().json();
    return isApiErrorBody(parsed) ? parsed : null;
  } catch {
    return null;
  }
};

const normalizeError = async (error: unknown): Promise<unknown> => {
  if (isHTTPError(error)) {
    const body = await readErrorBody(error.response);
    return new HttpError(error.response.status, body, error.response);
  }
  // 서버에서 만든 에러가 아닌 경우 에러 그대로 반환
  return error;
};

export const httpClient = async <T>(
  method: HttpMethod,
  url: string,
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
