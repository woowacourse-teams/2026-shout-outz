import { infiniteQueryOptions, queryOptions } from '@tanstack/react-query';

import { httpClient, type ApiSuccessBody } from '@/utils/client';

// 서버의 사용자 채용공고 API(JobPostingHttpApi)와 같은 모양이다.
// src/api/generated/schema.ts에 아직 이 API가 없어 백엔드 DTO를 보고 옮겨 적었다. generate:api를 다시 돌리면 그 타입으로 바꾼다.

export type JobRole = 'BACKEND' | 'FRONTEND' | 'ANDROID' | 'IOS' | 'DEVOPS' | 'DATA' | 'AI' | 'ETC';

/** 역량이 공고의 어느 부분에서 나왔는지. 자격 요건, 우대 사항, 주요 업무나 기술 스택 */
export type RequirementCategory = 'REQUIRED' | 'PREFERRED' | 'DUTY_OR_STACK';

export type RecommendationVerdict = 'MATCH' | 'PARTIAL';

export interface JobPostingListItem {
  jobPostingId: number;
  companyName: string;
  // MOCK-API: companyLogoUrl, tags는 서버 명세에 없는 필드다. 프로토타입 카드에 회사 로고와 해시태그가 있어 추가했다. 서버에 추가를 요청해야 한다.
  companyLogoUrl: string | null;
  /** 카드에 보여 줄 해시태그. 예: Server, Kotlin */
  tags: string[];
  jobRole: JobRole;
  employmentType: string | null;
  careerRequirement: string | null;
  title: string;
  /** YYYY-MM-DD. 없으면 상시 채용 */
  deadline: string | null;
  requirementCount: number;
  requirementNames: string[];
}

export interface JobPostingListMeta {
  nextCursor: string | null;
  hasNext: boolean;
  totalCount: number;
  jobRoleCounts: { jobRole: JobRole; count: number }[];
  allCount: number;
}

export interface JobRequirement {
  requirementId: number;
  displayOrder: number;
  name: string;
  category: RequirementCategory;
  summary: string | null;
  recommendationCount: number;
}

export interface JobPostingDetail {
  jobPostingId: number;
  companyName: string;
  // MOCK-API: 서버 명세에 없는 필드다. 목록과 같은 이유로 추가했다.
  companyLogoUrl: string | null;
  jobRole: JobRole;
  employmentType: string | null;
  careerRequirement: string | null;
  title: string;
  /** YYYY-MM-DD. 없으면 상시 채용 */
  deadline: string | null;
  sourceUrl: string;
  /** 공고에 적힌 순서 */
  requirements: JobRequirement[];
}

/** 역량별 추천 자료. source, contentType은 데이터 파이프라인 코드 그대로다. */
export interface JobRequirementRecommendation {
  // MOCK-API: 백엔드와 resourceId로 추가하기로 합의했고 서버 반영 전이다.
  // ASSUMPTION: 타입은 정해지지 않아 탐색 자료 ID(예: blog-post|post:…|v1)와 같은 문자열로 둔다. 자료 요약 모달이 이 ID로 요약을 불러온다.
  resourceId: string;
  verdict: RecommendationVerdict;
  /** blog-post, github-pr, github-wiki, youtube-tekotalk 등 */
  source: string;
  title: string;
  url: string;
  description: string | null;
  /** ISO 시각 */
  publishedAt: string | null;
  contentType: string | null;
  teamName: string | null;
  authorDisplay: string | null;
}

// MOCK-API: 상세 오른쪽의 "커뮤니티에서 나온 고민"은 서버 API가 없어 모양을 임의로 정했다.
export interface JobPostingCommunityFeed {
  feedId: number;
  /** 질문, 고민 등 글 종류 */
  type: string;
  categoryName: string;
  title: string;
  content: string;
  authorDisplay: string;
  commentCount: number;
  likeCount: number;
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

export async function fetchJobPostingCommunityFeeds(jobPostingId: number, signal?: AbortSignal) {
  const response = await httpClient<ApiSuccessBody<JobPostingCommunityFeed[]>>(
    `/api/v1/job-postings/${jobPostingId}/community-feeds`,
    { method: 'get', signal },
  );
  if (!response?.data) throw new Error('커뮤니티 고민 응답이 비어 있습니다.');
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

export function jobPostingCommunityFeedsQuery(jobPostingId: number) {
  return queryOptions({
    queryKey: ['job-posting', jobPostingId, 'community-feeds'],
    queryFn: ({ signal }) => fetchJobPostingCommunityFeeds(jobPostingId, signal),
  });
}
