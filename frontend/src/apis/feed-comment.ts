import { infiniteQueryOptions, mutationOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import { nextCursor, type Envelope } from '@/apis/feed';
export interface FeedComment {
  id: number;
  content: string;
  author: { userId: number; name: string; avatarUrl: string | null };
  parentId: number | null;
  createdAt: string;
  updatedAt: string;
  editable: boolean;
  edited?: boolean;
}
export function commentsQuery(postId: number, viewer: number | null) {
  return infiniteQueryOptions({
    queryKey: ['feed-comments', postId, { size: 20, viewer }],
    initialPageParam: undefined as string | undefined,
    queryFn: async ({ pageParam, signal }) => {
      const result = await httpClient<Envelope<{ items: FeedComment[] }>>(
        `/api/v1/posts/${postId}/comments`,
        {
          method: 'get',
          signal,
          searchParams: { sort: 'LATEST', size: 20, ...(pageParam ? { cursor: pageParam } : {}) },
        },
      );
      if (!result) throw new Error('댓글 응답이 비어 있습니다.');
      return result;
    },
    getNextPageParam: (last, _pages, _param, params) => nextCursor(last, params),
  });
}
export type CommentChange = {
  method: 'post' | 'patch' | 'delete';
  commentId?: number;
  content?: string;
};
export const commentMutation = (postId: number) =>
  mutationOptions({
    retry: false,
    mutationFn: (input: CommentChange) =>
      httpClient(
        `/api/v1/posts/${postId}/comments${input.commentId === undefined ? '' : `/${input.commentId}`}`,
        {
          method: input.method,
          ...(input.method === 'delete' ? {} : { json: { content: input.content } }),
        },
      ),
  });
