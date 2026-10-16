import { infiniteQueryOptions, queryOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import { nextCursor } from '@/apis/feed';
import type {
  InterviewFindAllSuccessResponse,
  InterviewFindSuccessResponse,
} from '@/api/generated/schema';
import type { InterviewDetailData, InterviewListItemData } from '@/types/api';

export type Interview = InterviewDetailData;
export type InterviewListItem = InterviewListItemData;

export async function fetchInterview(interviewId: number, signal?: AbortSignal) {
  const response = await httpClient<InterviewFindSuccessResponse>(
    `/api/v1/interviews/${interviewId}`,
    { method: 'get', signal },
  );

  if (!response?.data) throw new Error('인터뷰 응답이 비어 있습니다.');
  return response.data;
}

export function interviewQuery(interviewId: number) {
  return queryOptions({
    queryKey: ['interview', interviewId],
    queryFn: ({ signal }) => fetchInterview(interviewId, signal),
  });
}

interface FetchInterviewsParams {
  cursor?: string;
  size: number;
  signal?: AbortSignal;
}

export async function fetchInterviews({ cursor, size, signal }: FetchInterviewsParams) {
  const response = await httpClient<InterviewFindAllSuccessResponse>('/api/v1/interviews', {
    method: 'get',
    signal,
    searchParams: { size, ...(cursor ? { cursor } : {}) },
  });

  if (!response) throw new Error('인터뷰 응답이 비어 있습니다.');
  return {
    ...response,
    meta: response.meta ?? { hasNext: false, nextCursor: null, totalCount: 0 },
  };
}

export function interviewsQuery(size = 20) {
  return infiniteQueryOptions({
    queryKey: ['interviews', { size }],
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam, signal }) => fetchInterviews({ cursor: pageParam, size, signal }),
    getNextPageParam: (last, _pages, _param, params) => nextCursor(last, params),
  });
}
