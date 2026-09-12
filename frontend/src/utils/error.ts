import {
  isNetworkError,
  isTimeoutError,
  NetworkError,
  TimeoutError,
  isHTTPError,
  HTTPError,
} from 'ky';

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

export const isApiErrorBody = (value: unknown): value is ApiErrorBody =>
  typeof value === 'object' &&
  value !== null &&
  (value as ApiErrorBody).status === 'error' &&
  typeof (value as ApiErrorBody).code === 'string' &&
  typeof (value as ApiErrorBody).message === 'string';

// 사용자에게 안내할 수 있는 에러. false면 우리 코드의 버그
export type ApiError = HTTPError | NetworkError | TimeoutError;

export const isApiError = (error: unknown): error is ApiError =>
  isHTTPError(error) || isNetworkError(error) || isTimeoutError(error);

// 우리 서비스의 서버가 내려준 에러 검사
export const isApiResponseError = (
  error: unknown,
): error is HTTPError<ApiErrorBody> & { data: ApiErrorBody } =>
  isHTTPError(error) && isApiErrorBody(error.data);
