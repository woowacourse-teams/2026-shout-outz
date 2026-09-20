import { infiniteQueryOptions, mutationOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import { nextCursor } from '@/apis/feed';
import type { FeedCommentFindAllSuccessResponse } from '@/api/generated/schema';
import type { CursorMeta } from '@/types/api';
import type { FeedComment } from '@/types/feed';

export type { FeedComment };

/** 댓글 목록은 meta가 optional이라 봉투를 따로 둔다. */
interface CommentsPage {
  status: string;
  data: FeedComment[];
  meta: CursorMeta;
}

interface FetchCommentsParams {
  feedId: number;
  cursor?: string;
  size: number;
  signal?: AbortSignal;
}

export async function fetchComments({ feedId, cursor, size, signal }: FetchCommentsParams) {
  const response = await httpClient<FeedCommentFindAllSuccessResponse>(
    `/api/v1/feeds/${feedId}/comments`,
    {
      method: 'get',
      signal,
      searchParams: { sort: 'LATEST', size, ...(cursor ? { cursor } : {}) },
    },
  );

  if (!response) throw new Error('댓글 응답이 비어 있습니다.');
  return {
    ...response,
    meta: response.meta ?? { hasNext: false, nextCursor: null },
  } satisfies CommentsPage;
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
