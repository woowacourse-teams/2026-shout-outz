import { httpClient } from '@/utils/client';

interface ReactionEnvelope<T> {
  status: string;
  data: T;
}

export interface FeedLikeResult {
  active: boolean;
  likeCount: number;
}

export interface CommentAgreeResult {
  active: boolean;
  agreeCount: number;
}

export interface ProjectLikeResult {
  active: boolean;
  likeCount: number;
}

export async function setFeedLike(feedId: number, active: boolean) {
  const response = await httpClient<ReactionEnvelope<FeedLikeResult>>(
    `/api/v1/feeds/${feedId}/reactions/LIKE`,
    { method: active ? 'put' : 'delete' },
  );
  if (!response) throw new Error('피드 좋아요 응답이 비어 있습니다.');
  return response.data;
}

export async function setFeedCommentAgree(feedId: number, commentId: number, active: boolean) {
  const response = await httpClient<ReactionEnvelope<CommentAgreeResult>>(
    `/api/v1/feeds/${feedId}/comments/${commentId}/reactions/AGREE`,
    { method: active ? 'put' : 'delete' },
  );
  if (!response) throw new Error('댓글 공감 응답이 비어 있습니다.');
  return response.data;
}

export async function setProjectLike(projectId: number, active: boolean) {
  const response = await httpClient<ReactionEnvelope<ProjectLikeResult>>(
    `/api/v1/projects/${projectId}/reactions/LIKE`,
    { method: active ? 'put' : 'delete' },
  );
  if (!response) throw new Error('프로젝트 좋아요 응답이 비어 있습니다.');
  return response.data;
}
