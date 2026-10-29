import { infiniteQueryOptions, mutationOptions } from '@tanstack/react-query';
import { httpClient } from '@/utils/client';
import { nextCursor } from '@/apis/feed';
import type {
  FeedCommentFindAllSuccessResponse,
  MagazineCommentFindAllSuccessResponse,
} from '@/api/generated/schema';
import type { CursorMeta } from '@/types/api';
import type { FeedComment } from '@/types/feed';

export type { FeedComment };

/** 댓글을 다는 대상. 피드와 매거진은 같은 구조의 댓글 API를 쓴다(`/api/v1/{대상}/{id}/comments`). */
export type CommentTarget = 'feeds' | 'magazines';

const COMMENT_QUERY_KEY: Record<CommentTarget, string> = {
  feeds: 'feed-comments',
  magazines: 'magazine-comments',
};

export const commentsQueryKey = (target: CommentTarget, targetId: number) => [
  COMMENT_QUERY_KEY[target],
  targetId,
];

/** 댓글 목록은 meta가 optional이라 봉투를 따로 둔다. */
interface CommentsPage {
  status: string;
  data: FeedComment[];
  meta: CursorMeta;
}

interface FetchCommentsParams {
  target?: CommentTarget;
  feedId: number;
  cursor?: string;
  size: number;
  signal?: AbortSignal;
}

export async function fetchComments({
  target = 'feeds',
  feedId,
  cursor,
  size,
  signal,
}: FetchCommentsParams) {
  const response = await httpClient<
    FeedCommentFindAllSuccessResponse | MagazineCommentFindAllSuccessResponse
  >(`/api/v1/${target}/${feedId}/comments`, {
    method: 'get',
    signal,
    searchParams: { sort: 'OLDEST', size, ...(cursor ? { cursor } : {}) },
  });

  if (!response) throw new Error('댓글 응답이 비어 있습니다.');
  return {
    ...response,
    meta: response.meta ?? { hasNext: false, nextCursor: null },
  } satisfies CommentsPage;
}

export function commentsQuery(
  feedId: number,
  viewer: number | null,
  target: CommentTarget = 'feeds',
) {
  return infiniteQueryOptions({
    queryKey: [...commentsQueryKey(target, feedId), { size: 20, viewer }],
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam, signal }) =>
      fetchComments({ target, feedId, cursor: pageParam, size: 20, signal }),
    getNextPageParam: (last, _pages, _param, params) => nextCursor(last, params),
  });
}
export type CommentChange = {
  method: 'post' | 'patch' | 'delete';
  commentId?: number;
  content?: string;
  isAnonymous?: boolean;
  parentId?: number;
};

export function changeComment(
  feedId: number,
  input: CommentChange,
  target: CommentTarget = 'feeds',
) {
  return httpClient(
    `/api/v1/${target}/${feedId}/comments${input.commentId === undefined ? '' : `/${input.commentId}`}`,
    {
      method: input.method,
      ...(input.method === 'delete'
        ? {}
        : {
            json: {
              content: input.content,
              ...(input.method === 'post'
                ? {
                    isAnonymous: input.isAnonymous ?? false,
                    ...(input.parentId === undefined ? {} : { parentId: input.parentId }),
                  }
                : {}),
            },
          }),
    },
  );
}

export const commentMutation = (feedId: number, target: CommentTarget = 'feeds') =>
  mutationOptions({
    retry: false,
    mutationFn: (input: CommentChange) => changeComment(feedId, input, target),
  });
