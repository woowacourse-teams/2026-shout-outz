import { isNetworkError, NetworkError } from 'ky';

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
  typeof (value as ApiErrorBody).code === 'string' &&
  typeof (value as ApiErrorBody).message === 'string';

export class HttpError extends Error {
  constructor(
    readonly status: number,
    readonly body: ApiErrorBody | null,
    readonly response: Response,
  ) {
    super(body?.message ?? `요청이 실패했습니다 (HTTP ${status})`);
    this.name = 'HttpError';
  }

  get code(): string | null {
    return this.body?.code ?? null;
  }

  get details(): ApiErrorDetail[] {
    return this.body?.details ?? [];
  }

  get isClientError(): boolean {
    return this.status >= 400 && this.status < 500;
  }

  get isServerError(): boolean {
    return this.status >= 500;
  }
}

export type ApiError = HttpError | NetworkError;

export const isApiError = (error: unknown): error is ApiError =>
  error instanceof HttpError || isNetworkError(error);
