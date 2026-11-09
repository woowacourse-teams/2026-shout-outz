import { http, HttpResponse } from 'msw';

import type { Material } from '@/apis/explore';
import type {
  JobPostingCommunityFeed,
  JobPostingDetail,
  JobPostingListItem,
  JobRequirementRecommendation,
  JobRole,
} from '@/apis/job-posting';
import { collectMaterials } from '@/api/mock/explore';

// 서버의 사용자 채용공고 API 명세(목록, 상세, 역량별 자료)에 맞춘 목 응답이다.
// 공고와 역량은 프로토타입 예시이고, 역량별 자료는 소개 페이지에 저장해 둔 실제 자료를 주제로 묶어 돌려준다.

interface MockRequirement {
  name: string;
  /** 자료를 가져올 탐색 주제. 비어 있으면 추천 자료가 없는 역량이다. */
  topics: string[];
}

interface MockJobPosting {
  jobPostingId: number;
  companyName: string;
  jobRole: JobRole;
  title: string;
  deadline: string | null;
  sourceUrl: string;
  requirements: MockRequirement[];
}

const JOB_POSTINGS: MockJobPosting[] = [
  {
    jobPostingId: 1,
    companyName: '토스뱅크',
    jobRole: 'BACKEND',
    title: 'Server Developer (Product)',
    deadline: null,
    sourceUrl: 'https://toss.im/career/jobs',
    requirements: [
      { name: 'Kotlin, Spring', topics: ['N+1 문제'] },
      { name: '동시성 제어', topics: ['동시성 제어/락'] },
      { name: 'DB 설계, 쿼리 튜닝', topics: ['인덱스/쿼리 최적화'] },
      { name: '무중단 배포', topics: ['무중단 배포'] },
      { name: '실시간 통신', topics: ['실시간 통신(WebSocket/SSE)'] },
      { name: '모니터링, 장애 대응', topics: [] },
      { name: '협업, 코드 리뷰', topics: [] },
    ],
  },
  {
    jobPostingId: 2,
    companyName: '토스증권',
    jobRole: 'FRONTEND',
    title: 'Frontend Developer',
    deadline: null,
    sourceUrl: 'https://toss.im/career/jobs',
    requirements: [
      { name: 'React, Next.js, TypeScript', topics: ['React 렌더링/훅'] },
      { name: '실시간 데이터 처리', topics: ['실시간 통신(WebSocket/SSE)'] },
      { name: '렌더링 성능 최적화', topics: ['React 렌더링/훅'] },
      { name: '상태관리', topics: [] },
      { name: '디자인 시스템, 컴포넌트 제작', topics: [] },
      { name: '빌드, 패키지, 번들링 최적화', topics: [] },
      { name: '협업, CI, 코드 리뷰, 테스트 자동화', topics: ['무중단 배포'] },
    ],
  },
];

const RECOMMENDATION_LIMIT = 6;

const SOURCE_BY_KIND: Record<Material['kind'], string> = {
  BLOG: 'blog-post',
  PR: 'github-pr',
  TEAM_DOC: 'github-wiki',
  TECH_TALK: 'youtube-tekotalk',
};

const requirementIdOf = (jobPostingId: number, index: number) => jobPostingId * 100 + index + 1;

const recommendationsOf = (requirement: MockRequirement): JobRequirementRecommendation[] =>
  collectMaterials(requirement.topics)
    .slice(0, RECOMMENDATION_LIMIT)
    .map((material, index) => ({
      verdict: index < 3 ? 'MATCH' : 'PARTIAL',
      source: SOURCE_BY_KIND[material.kind],
      title: material.title,
      url: material.url,
      description: material.summary,
      publishedAt: material.publishedAt ? `${material.publishedAt}T00:00:00Z` : null,
      contentType: null,
      teamName: material.project?.name ?? null,
      authorDisplay: material.speaker,
    }));

const toListItem = (posting: MockJobPosting): JobPostingListItem => ({
  jobPostingId: posting.jobPostingId,
  companyName: posting.companyName,
  jobRole: posting.jobRole,
  employmentType: '정규직',
  careerRequirement: '신입',
  title: posting.title,
  deadline: posting.deadline,
  requirementCount: posting.requirements.length,
  requirementNames: posting.requirements.map((requirement) => requirement.name),
});

const toDetail = (posting: MockJobPosting): JobPostingDetail => ({
  ...toListItem(posting),
  sourceUrl: posting.sourceUrl,
  requirements: posting.requirements.map((requirement, index) => ({
    requirementId: requirementIdOf(posting.jobPostingId, index),
    displayOrder: index + 1,
    name: requirement.name,
    category: 'REQUIRED',
    summary: null,
    recommendationCount: recommendationsOf(requirement).length,
  })),
});

const matchesKeyword = (posting: MockJobPosting, keyword: string) => {
  const key = keyword.toLowerCase();
  return [
    posting.companyName,
    posting.title,
    posting.jobRole,
    ...posting.requirements.map((r) => r.name),
  ].some((text) => text.toLowerCase().includes(key));
};

export const getJobPostings = (keyword: string) => {
  const postings = keyword
    ? JOB_POSTINGS.filter((posting) => matchesKeyword(posting, keyword))
    : JOB_POSTINGS;
  const roles: JobRole[] = ['BACKEND', 'FRONTEND', 'ANDROID', 'IOS', 'DEVOPS', 'DATA', 'AI', 'ETC'];
  const jobRoleCounts = roles.map((jobRole) => ({
    jobRole,
    count: postings.filter((posting) => posting.jobRole === jobRole).length,
  }));

  return {
    data: postings.map(toListItem),
    meta: {
      nextCursor: null,
      hasNext: false,
      totalCount: postings.length,
      jobRoleCounts,
      allCount: postings.length,
    },
  };
};

// MOCK-API: 서버에 없는 "커뮤니티에서 나온 고민" 목 응답이다. 글 내용은 프로토타입 예시다.
const COMMUNITY_FEEDS: JobPostingCommunityFeed[] = [
  {
    feedId: 1,
    type: '질문',
    categoryName: '취업 준비',
    title: '금융권 취업 방향성',
    content:
      '안녕하세요! 현재 금융권을 우선적으로 취업준비하고 있는 학생입니다. 금융권 취업에 앞서 중요한 고려 요소들에 대해 여쭤보고 싶습니다. 1. 도메인 지식이 중요한가요? 2. 금융권은 취업에 앞서 자격증과 정량적 기준으로 서류 합격이 판가름된다는 의견을 들었는데, 실제 취업하신분들이 느꼈던 점이 궁금합니다.',
    authorDisplay: '익명',
    commentCount: 4,
    likeCount: 5,
  },
  {
    feedId: 2,
    type: '질문',
    categoryName: '취업 준비',
    title: '금융권 준비에 투운사가 필요한가요?',
    content:
      '오늘 투운사 접수일이라서 금융권 IT 취업에도 투운사가 있다면 이점이 있는지 궁금합니다. 사실 무관한 것 같기도 하면서, 도메인 관련된 지식에 어필이 될까 해서 여쭤봅니다!',
    authorDisplay: '익명',
    commentCount: 1,
    likeCount: 1,
  },
  {
    feedId: 3,
    type: '질문',
    categoryName: '이력서, 자소서',
    title: '포트폴리오, 프로젝트마다 어떤 내용을 담으셨나요?',
    content:
      '안녕하세요! 포트폴리오를 준비하면서 프로젝트 경험을 어떻게 정리할지 고민하고 있습니다. 구현한 기능을 나열하기보다는 제가 고민하고 해결한 과정을 보여주고 싶은데, 어디까지 자세히 써야 할지 어렵더라고요.',
    authorDisplay: '익명',
    commentCount: 2,
    likeCount: 4,
  },
];

const notFound = (message: string) =>
  HttpResponse.json({ status: 'error', code: 'JOB_POSTING_NOT_FOUND', message }, { status: 404 });

const findPosting = (jobPostingId: unknown) =>
  JOB_POSTINGS.find((posting) => posting.jobPostingId === Number(jobPostingId));

export const jobPostingHandlers = [
  http.get('/api/v1/job-postings', ({ request }) => {
    const keyword = new URL(request.url).searchParams.get('keyword')?.trim() ?? '';
    return HttpResponse.json({ status: 'success', ...getJobPostings(keyword) });
  }),
  http.get('/api/v1/job-postings/:jobPostingId', ({ params }) => {
    const posting = findPosting(params.jobPostingId);
    if (!posting) return notFound('채용공고를 찾을 수 없습니다.');
    return HttpResponse.json({ status: 'success', data: toDetail(posting) });
  }),
  http.get(
    '/api/v1/job-postings/:jobPostingId/requirements/:requirementId/recommendations',
    ({ params }) => {
      const posting = findPosting(params.jobPostingId);
      const index = posting?.requirements.findIndex(
        (_, i) => requirementIdOf(posting.jobPostingId, i) === Number(params.requirementId),
      );
      if (!posting || index === undefined || index < 0)
        return notFound('채용공고 역량을 찾을 수 없습니다.');
      return HttpResponse.json({
        status: 'success',
        data: recommendationsOf(posting.requirements[index]!),
      });
    },
  ),
  http.get('/api/v1/job-postings/:jobPostingId/community-feeds', ({ params }) => {
    if (!findPosting(params.jobPostingId)) return notFound('채용공고를 찾을 수 없습니다.');
    return HttpResponse.json({ status: 'success', data: COMMUNITY_FEEDS });
  }),
];
