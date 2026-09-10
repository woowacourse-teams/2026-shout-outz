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

const requestEnvelope = async (
  method: HttpMethod,
  url: string,
  options: Options,
): Promise<ApiSuccessBody<unknown> | null> => {
  try {
    const response = await kyInstance(url, { ...options, method });

    // TODO DELETE가 204인 것도 있고 200인 것도 있어서 논의 필요
    if (response.status === 204 || response.headers.get('content-length') === '0') {
      return null;
    }
    const body: unknown = await response.json();

    if (!isApiSuccessBody(body)) {
      throw new Error(`응답이 공통 규격을 따르지 않습니다: ${method.toUpperCase()} ${url}`);
    }

    return body;
  } catch (error) {
    throw await normalizeError(error);
  }
};

export const httpClient = async <T>(
  method: HttpMethod,
  url: string,
  options: Options = {},
): Promise<T> => {
  const body = await requestEnvelope(method, url, options);

  return (body === null ? undefined : body.data) as T;
};

export const httpClientWithMeta = async <T, M>(
  method: HttpMethod,
  url: string,
  options: Options = {},
): Promise<{ data: T; meta: M }> => {
  const body = await requestEnvelope(method, url, options);

  if (body?.meta === undefined) {
    throw new Error(`meta가 없는 응답입니다: ${method.toUpperCase()} ${url}`);
  }

  return { data: body.data as T, meta: body.meta as M };
};
