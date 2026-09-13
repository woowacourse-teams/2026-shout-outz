import { queryOptions } from '@tanstack/react-query';
import ky from 'ky';
import { getApiUrl } from '@/api/url';

export interface ProjectMember {
  userId: number;
  displayName: string;
  cohort: number;
  track: string;
  avatarUrl: string | null;
}

export interface ProjectDetail {
  id: number;
  slug: string;
  title: string;
  teamName: string;
  tagline: string;
  cohort: number;
  thumbnailUrl: string | null;
  descriptionMd: string;
  githubRepositoryUrl: string | null;
  deploymentUrl: string | null;
  serviceStatus: string;
  likeCount: number;
  bookmarkCount: number;
  techTags: { id: number; displayName: string }[];
  members: ProjectMember[];
}

interface ProjectDetailResponse extends ProjectDetail {
  approvalStatus: string;
  rejectReason: string | null;
  likedByMe: boolean;
  bookmarkedByMe: boolean;
  viewCount: number;
  starCount: number;
  commentCount: number;
  createdAt: string;
  updatedAt: string;
}

export class ProjectNotFoundError extends Error {
  constructor() {
    super('프로젝트가 없거나 접근할 수 없습니다.');
  }
}

async function requestProject(id: string, signal?: AbortSignal, personalized = false) {
  if (!/^[1-9]\d*$/.test(id)) throw new ProjectNotFoundError();
  const response = await ky.get(getApiUrl(`/api/v1/projects/${id}`), {
    signal,
    retry: 0,
    credentials: personalized ? 'same-origin' : 'omit',
    throwHttpErrors: false,
  });
  if (response.status === 404) throw new ProjectNotFoundError();
  if (!response.ok) throw new Error('프로젝트를 불러오지 못했습니다.');
  const body = await response.json<{ status: string; data: ProjectDetailResponse }>();
  if (body.status !== 'success' || !body.data || String(body.data.id) !== id) {
    throw new Error('프로젝트 상세 응답을 확인할 수 없습니다.');
  }
  // 정적 페이지와 공개 조회에는 승인된 프로젝트만 사용한다.
  if (!personalized && body.data.approvalStatus !== 'APPROVED') {
    throw new ProjectNotFoundError();
  }
  return body.data;
}

export async function fetchProjectDetail(id: string, signal?: AbortSignal): Promise<ProjectDetail> {
  const data = await requestProject(id, signal);
  // select는 원본 Query 캐시를 바꾸지 않으므로, 저장 전에 공개 필드만 추출한다.
  return {
    id: data.id,
    slug: data.slug,
    title: data.title,
    teamName: data.teamName,
    tagline: data.tagline,
    cohort: data.cohort,
    thumbnailUrl: data.thumbnailUrl,
    descriptionMd: data.descriptionMd,
    githubRepositoryUrl: data.githubRepositoryUrl,
    deploymentUrl: data.deploymentUrl,
    serviceStatus: data.serviceStatus,
    likeCount: data.likeCount,
    bookmarkCount: data.bookmarkCount,
    techTags: data.techTags,
    members: data.members,
  };
}

export const projectDetailQueryOptions = (id: string) =>
  queryOptions({
    queryKey: ['project-detail', id],
    queryFn: ({ signal }) => fetchProjectDetail(id, signal),
    staleTime: 60_000,
  });

export const projectReactionsQueryOptions = (id: string) =>
  queryOptions({
    queryKey: ['project-reactions', id],
    queryFn: async ({ signal }) => {
      const data = await requestProject(id, signal, true);
      return {
        likedByMe: data.likedByMe,
        bookmarkedByMe: data.bookmarkedByMe,
        likeCount: data.likeCount,
        bookmarkCount: data.bookmarkCount,
      };
    },
    retry: false,
  });
