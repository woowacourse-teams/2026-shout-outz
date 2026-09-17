import { infiniteQueryOptions, mutationOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import { nextCursor, type Envelope } from '@/apis/feed';
export interface FeedComment {
  id: number;
  content: string | null;
  author: { userId: number; displayName: string; avatarImageId: number | null };
  parentId: number | null;
  createdAt: string;
  updatedAt: string;
  editable: boolean;
  edited: boolean;
  deleted: boolean;
}

interface FetchCommentsParams {
  feedId: number;
  cursor?: string;
  size: number;
  signal?: AbortSignal;
}

export async function fetchComments({ feedId, cursor, size, signal }: FetchCommentsParams) {
  const response = await httpClient<Envelope<FeedComment[]>>(`/api/v1/feeds/${feedId}/comments`, {
    method: 'get',
    signal,
    searchParams: { sort: 'LATEST', size, ...(cursor ? { cursor } : {}) },
  });

  if (!response) throw new Error('댓글 응답이 비어 있습니다.');
  return response;
}

export function commentsQuery(feedId: number, viewer: number | null) {
  return infiniteQueryOptions({
    queryKey: ['feed-comments', feedId, { size: 20, viewer }],
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam, signal }) =>
      fetchComments({ feedId, cursor: pageParam, size: 20, signal }),
    getNextPageParam: (last, _pages, _param, params) => nextCursor(last, params),
  });
}
export type CommentChange = {
  method: 'post' | 'patch' | 'delete';
  commentId?: number;
  content?: string;
};

export function changeComment(feedId: number, input: CommentChange) {
  return httpClient(
    `/api/v1/feeds/${feedId}/comments${input.commentId === undefined ? '' : `/${input.commentId}`}`,
    {
      method: input.method,
      ...(input.method === 'delete' ? {} : { json: { content: input.content } }),
    },
  );
}

export const commentMutation = (feedId: number) =>
  mutationOptions({
    retry: false,
    mutationFn: (input: CommentChange) => changeComment(feedId, input),
  });
