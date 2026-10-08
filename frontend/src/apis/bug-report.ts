import { mutationOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import type {
  BugReportCreateRequest,
  BugReportCreateSuccessResponse,
} from '@/api/generated/schema';

/** 서버 `BugReportCreateRequest`의 `content` 최대 길이 (Unicode code point 기준) */
export const BUG_REPORT_MAX_LENGTH = 5000;

/** 로그인하지 않아도 보낼 수 있다. */
export async function createBugReport(input: BugReportCreateRequest) {
  const response = await httpClient<BugReportCreateSuccessResponse>('/api/v1/bug-reports', {
    method: 'post',
    json: input,
  });
  if (!response) throw new Error('버그 제보 결과를 확인하지 못했습니다.');
  return response.data;
}

export const createBugReportMutation = mutationOptions({
  mutationFn: createBugReport,
  retry: false,
});
