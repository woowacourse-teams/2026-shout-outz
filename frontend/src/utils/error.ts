import { isHTTPError, HTTPError } from 'ky';

export interface ApiErrorDetail {
  field: string;
  message: string;
}

export interface ApiErrorBody {
  status: 'error';
  code: string;
  message: string;
  details?: ApiErrorDetail[];
}

const isApiErrorBody = (value: unknown): value is ApiErrorBody =>
  typeof value === 'object' &&
  value !== null &&
  (value as ApiErrorBody).status === 'error' &&
  typeof (value as ApiErrorBody).code === 'string' &&
  typeof (value as ApiErrorBody).message === 'string';

// 우리 서비스의 서버가 내려준 에러 검사
export const isApiResponseError = (
  error: unknown,
): error is HTTPError<ApiErrorBody> & { data: ApiErrorBody } =>
  isHTTPError(error) && isApiErrorBody(error.data);
