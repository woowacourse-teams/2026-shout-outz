import { mutationOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';

/**
 * 버그 제보 요청·응답.
 *
 * dev 서버 OpenAPI 명세에 아직 빠져 있어 `generate:api`로 생기지 않는다. 백엔드
 * `BugReportCreateRequest`·`BugReportCreateResponse`를 보고 옮겼고, 명세에 들어오면 생성 타입으로 바꾼다.
 */
export interface BugReportCreateInput {
  /** 앞뒤 공백을 뺀 1~5000자 (Unicode code point 기준) */
  content: string;
}

export interface BugReportCreated {
  bugReportId: number;
  /** 접수 직후에는 OPEN, 처리하면 COMPLETED */
  status: 'OPEN' | 'COMPLETED';
  createdAt: string;
}

export const BUG_REPORT_MAX_LENGTH = 5000;

/** 로그인하지 않아도 보낼 수 있다. */
export async function createBugReport(input: BugReportCreateInput) {
  const response = await httpClient<{ status: 'success'; data: BugReportCreated }>(
    '/api/v1/bug-reports',
    { method: 'post', json: input },
  );
  if (!response) throw new Error('버그 제보 결과를 확인하지 못했습니다.');
  return response.data;
}

export const createBugReportMutation = mutationOptions({
  mutationFn: createBugReport,
  retry: false,
});
