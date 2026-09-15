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
  approvalStatus: string;
  rejectReason: string | null;
  likedByMe: boolean;
  bookmarkedByMe: boolean;
  viewCount: number;
  starCount: number;
  commentCount: number;
  createdAt: string;
  updatedAt: string;
  techTags: { id: number; displayName: string }[];
  members: ProjectMember[];
}

interface ProjectDetailResponse {
  status: string;
  data: ProjectDetail;
}

export async function fetchProjectDetail(
  id: string,
  signal?: AbortSignal,
): Promise<ProjectDetail> {
  const response = await ky
    .get(getApiUrl(`/api/v1/projects/${id}`), { signal, retry: 0, credentials: 'omit' })
    .json<ProjectDetailResponse>();

  return response.data;
}

export const projectDetailQueryOptions = (id: string) =>
  queryOptions({
    queryKey: ['project-detail', id],
    queryFn: ({ signal }) => fetchProjectDetail(id, signal),
    staleTime: 60_000,
  });
