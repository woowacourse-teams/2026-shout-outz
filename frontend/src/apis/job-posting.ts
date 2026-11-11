import { infiniteQueryOptions, queryOptions } from '@tanstack/react-query';

import type {
  JobPostingDetailSuccessResponse,
  JobPostingFindAllSuccessResponse,
  JobRequirementRecommendationsSuccessResponse,
} from '@/api/generated/schema';
import { httpClient, type ApiSuccessBody } from '@/utils/client';

// 서버의 사용자 채용공고 API(/api/v1/job-postings)와 같은 모양이다. 타입은 서버 명세에서 생성한 schema.ts를 쓴다.
//
// TODO(채용공고 후속 PR): 프로토타입 카드·상세에 있는 회사 로고(companyLogoUrl)와 해시태그(tags)는 서버 응답에 없어
// 화면에서 뺐다. 서버가 필드를 주면 schema.ts를 다시 생성하고, 따로 PR을 올려 목록 카드와 상세 머리에 다시 넣는다.

type GeneratedListItem = JobPostingFindAllSuccessResponse['data'][number];

export type JobRole = GeneratedListItem['jobRole'];

/**
 * 채용공고 목록 한 건.
 * 스키마는 requirementNames를 `(object | boolean | string | number)[]`로 뽑는다(생성기가 배열 원소 타입을 못 읽은 결과다).
 * 서버가 주는 값은 역량 이름이라 string[]로 좁혀 쓴다.
 */
export interface JobPostingListItem extends Omit<GeneratedListItem, 'requirementNames'> {
  requirementNames: string[];
}

export type JobPostingListMeta = JobPostingFindAllSuccessResponse['meta'];

export type JobPostingDetail = JobPostingDetailSuccessResponse['data'];

export type JobRequirement = JobPostingDetail['requirements'][number];

/** 역량이 공고의 어느 부분에서 나왔는지. 자격 요건, 우대 사항, 주요 업무나 기술 스택 */
export type RequirementCategory = JobRequirement['category'];

/** 서버는 verdict를 string으로 명세하지만 값은 MATCH, PARTIAL 둘이다. */
export type RecommendationVerdict = 'MATCH' | 'PARTIAL';

type GeneratedRecommendation = JobRequirementRecommendationsSuccessResponse['data'][number];

/** 역량별 추천 자료. source, contentType은 데이터 파이프라인 코드 그대로다. */
export interface JobRequirementRecommendation extends Omit<GeneratedRecommendation, 'verdict'> {
  verdict: RecommendationVerdict;
}

const PAGE_SIZE = 20;

export async function fetchJobPostings(
  { keyword, cursor }: { keyword: string; cursor?: string | null },
  signal?: AbortSignal,
) {
  const searchParams: Record<string, string | number> = { size: PAGE_SIZE };
  if (keyword) searchParams.keyword = keyword;
  if (cursor) searchParams.cursor = cursor;

  const response = await httpClient<ApiSuccessBody<JobPostingListItem[], JobPostingListMeta>>(
    '/api/v1/job-postings',
    { method: 'get', signal, searchParams },
  );
  if (!response?.data) throw new Error('채용공고 목록 응답이 비어 있습니다.');
  return { data: response.data, meta: response.meta };
}

export async function fetchJobPosting(jobPostingId: number, signal?: AbortSignal) {
  const response = await httpClient<ApiSuccessBody<JobPostingDetail>>(
    `/api/v1/job-postings/${jobPostingId}`,
    { method: 'get', signal },
  );
  if (!response?.data) throw new Error('채용공고 응답이 비어 있습니다.');
  return response.data;
}

export async function fetchRequirementRecommendations(
  jobPostingId: number,
  requirementId: number,
  signal?: AbortSignal,
) {
  const response = await httpClient<ApiSuccessBody<JobRequirementRecommendation[]>>(
    `/api/v1/job-postings/${jobPostingId}/requirements/${requirementId}/recommendations`,
    { method: 'get', signal },
  );
  if (!response?.data) throw new Error('추천 자료 응답이 비어 있습니다.');
  return response.data;
}

export function jobPostingsQuery(keyword: string) {
  return infiniteQueryOptions({
    queryKey: ['job-postings', { keyword }],
    queryFn: ({ pageParam, signal }) => fetchJobPostings({ keyword, cursor: pageParam }, signal),
    initialPageParam: null as string | null,
    getNextPageParam: (lastPage) => (lastPage.meta?.hasNext ? lastPage.meta.nextCursor : null),
  });
}

export function jobPostingQuery(jobPostingId: number) {
  return queryOptions({
    queryKey: ['job-posting', jobPostingId],
    queryFn: ({ signal }) => fetchJobPosting(jobPostingId, signal),
  });
}

export function requirementRecommendationsQuery(jobPostingId: number, requirementId: number) {
  return queryOptions({
    queryKey: ['job-posting', jobPostingId, 'requirement', requirementId, 'recommendations'],
    queryFn: ({ signal }) => fetchRequirementRecommendations(jobPostingId, requirementId, signal),
  });
}
