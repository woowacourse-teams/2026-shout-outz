import { useState } from 'react';
import {
  useMutation,
  useSuspenseQuery,
  useQueryClient,
  useSuspenseInfiniteQuery,
} from '@tanstack/react-query';
import {
  commentsQuery,
  commentMutation,
  type CommentChange,
  type FeedComment,
} from '@/apis/feed-comment';
import { sessionQuery } from '@/apis/session';
import { Button } from '@/components/Button';
import { Avatar } from '@/components/Avatar';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { formatRelativeTime } from '@/utils/date';
import { getApiErrorMessage } from '@/utils/error';

export function Comments({ postId }: { postId: number }) {
  return (
    <AsyncBoundary key={postId}>
      <CommentsWithSession postId={postId} />
    </AsyncBoundary>
  );
}

function CommentsWithSession({ postId }: { postId: number }) {
  const session = useSuspenseQuery(sessionQuery);
  const viewer = session.data.status === 'AUTHENTICATED' ? session.data.userId : null;

  return (
    <AsyncBoundary key={viewer ?? 'guest'}>
      <CommentList
        postId={postId}
        viewer={viewer}
        sessionReady={!session.isFetching && !session.isError}
      />
    </AsyncBoundary>
  );
}
function CommentList({
  postId,
  viewer,
  sessionReady,
}: {
  postId: number;
  viewer: number | null;
  sessionReady: boolean;
}) {
  const query = useSuspenseInfiniteQuery(commentsQuery(postId, viewer));
  const client = useQueryClient();
  const mutation = useMutation(commentMutation(postId));
  const [content, setContent] = useState('');
  const [message, setMessage] = useState('');
  const [failure, setFailure] = useState('');
  const [refreshError, setRefreshError] = useState<unknown | null>(null);
  const items = [
    ...new Map(
      query.data.pages.flatMap((page) => page.data.items).map((item) => [item.id, item]),
    ).values(),
  ];
  async function change(input: CommentChange, onSuccess: () => void) {
    setFailure('');
    setMessage('');
    try {
      await mutation.mutateAsync(input);
    } catch (error) {
      setFailure(getApiErrorMessage(error));
      return;
    }
    onSuccess();
    setMessage(input.method === 'delete' ? '댓글을 삭제했습니다.' : '댓글을 저장했습니다.');
    try {
      await client.invalidateQueries(
        { queryKey: ['feed-comments', postId] },
        { throwOnError: true },
      );
      setRefreshError(null);
    } catch (error) {
      setRefreshError(error);
    }
  }
  return (
    <div className="space-y-6">
      {viewer !== null ? (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (content.trim() && !mutation.isPending)
              void change({ method: 'post', content }, () => setContent(''));
          }}
          className="flex items-center gap-2 rounded-lg border border-gray-200 p-1"
        >
          <label className="sr-only" htmlFor={`new-comment-${postId}`}>
            댓글 남기기
          </label>
          <input
            id={`new-comment-${postId}`}
            type="text"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            disabled={mutation.isPending}
            placeholder="피드에 크루 댓글을 남겨보세요..."
            className="min-w-0 flex-1 bg-transparent px-3 py-2 text-sm text-gray-900 outline-none placeholder:text-gray-500"
          />
          <Button
            type="submit"
            aria-label="댓글 작성"
            disabled={!content.trim() || mutation.isPending || !sessionReady}
          >
            작성
          </Button>
        </form>
      ) : (
        <p className="rounded-lg bg-gray-50 p-3 text-sm text-gray-600">
          댓글을 작성하려면{' '}
          <a
            className="text-primary-600 underline"
            href={`${process.env.API_BASE_URL || ''}/oauth2/authorization/github`}
          >
            GitHub 로그인
          </a>
          이 필요합니다.
        </p>
      )}
      {message && (
        <p role="status" className="text-primary-600 text-sm">
          {message}
        </p>
      )}
      {failure && (
        <p role="alert" className="text-sm text-red-600">
          {failure}
        </p>
      )}
      {(refreshError !== null || (query.isRefetchError && !query.isFetchNextPageError)) && (
        <div role="alert" className="text-sm text-gray-600">
          {getApiErrorMessage(refreshError ?? query.error)}{' '}
          <Button
            size="sm"
            variant="outline"
            onClick={() =>
              void query.refetch().then((result) => setRefreshError(result.error ?? null))
            }
          >
            목록 새로고침
          </Button>
        </div>
      )}
      {items.length === 0 && <p className="py-4 text-sm text-gray-500">아직 댓글이 없습니다.</p>}
      <ul className="divide-y divide-gray-100">
        {items.map((item) => (
          <CommentItem
            key={item.id}
            item={item}
            canEdit={sessionReady && viewer === item.author.userId && item.editable}
            canDelete={sessionReady && viewer === item.author.userId}
            pending={mutation.isPending}
            change={change}
          />
        ))}
      </ul>
      {query.isFetchNextPageError && (
        <p role="alert" className="text-sm text-red-600">
          {getApiErrorMessage(query.error)}
        </p>
      )}
      {query.hasNextPage && (
        <div className="grid">
          <Button
            variant="outline"
            disabled={query.isFetching}
            onClick={() => void query.fetchNextPage()}
          >
            {query.isFetchingNextPage
              ? '불러오는 중…'
              : query.isFetchNextPageError
                ? '추가 댓글 다시 시도'
                : '댓글 더 보기'}
          </Button>
        </div>
      )}
    </div>
  );
}
function CommentItem({
  item,
  canEdit,
  canDelete,
  pending,
  change,
}: {
  item: FeedComment;
  canEdit: boolean;
  canDelete: boolean;
  pending: boolean;
  change: (input: CommentChange, done: () => void) => Promise<void>;
}) {
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [draft, setDraft] = useState(item.content);
  return (
    <li className="min-w-0 py-4 first:pt-0 last:pb-0">
      <div className="mb-2 flex min-w-0 items-center gap-2">
        <Avatar size="xs" src={item.author.avatarUrl ?? undefined} alt="" />
        <span className="min-w-0 truncate text-sm font-semibold text-gray-900">
          {item.author.name}
        </span>
        <time dateTime={item.createdAt} className="shrink-0 text-sm text-gray-500">
          {formatRelativeTime(item.createdAt)}
          {item.edited ? ' · 수정됨' : ''}
        </time>
      </div>
      {item.parentId !== null && (
        <p className="mb-1 text-xs text-gray-500">댓글 #{item.parentId}에 대한 답글</p>
      )}
      {editing && canEdit ? (
        <form
          className="space-y-2"
          onSubmit={(e) => {
            e.preventDefault();
            if (draft.trim() && !pending)
              void change({ method: 'patch', commentId: item.id, content: draft }, () =>
                setEditing(false),
              );
          }}
        >
          <label className="sr-only" htmlFor={`edit-${item.id}`}>
            댓글 수정 내용
          </label>
          <textarea
            id={`edit-${item.id}`}
            className="bg-background w-full rounded-lg border border-gray-200 p-3 text-sm text-gray-900"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            disabled={pending}
          />
          <Button size="sm" type="submit" disabled={pending || !draft.trim()}>
            수정 저장
          </Button>
          <Button size="sm" variant="ghost" disabled={pending} onClick={() => setEditing(false)}>
            취소
          </Button>
        </form>
      ) : (
        <p className="text-sm leading-6 break-words whitespace-pre-wrap text-gray-600">
          {item.content}
        </p>
      )}
      <div className="mt-1 flex gap-1">
        {canEdit && !editing && (
          <Button
            size="sm"
            variant="ghost"
            disabled={pending}
            onClick={() => {
              setDraft(item.content);
              setEditing(true);
            }}
          >
            수정
          </Button>
        )}
        {canDelete && (
          <Button size="sm" variant="ghost" disabled={pending} onClick={() => setDeleting(true)}>
            삭제
          </Button>
        )}
      </div>
      {deleting && canDelete && (
        <div role="group" aria-label="댓글 삭제 확인" className="mt-2 rounded-lg bg-gray-50 p-3">
          <p className="mb-2 text-sm text-gray-700">이 댓글을 삭제할까요?</p>
          <Button
            size="sm"
            disabled={pending}
            onClick={() =>
              void change({ method: 'delete', commentId: item.id }, () => setDeleting(false))
            }
          >
            삭제 확인
          </Button>
          <Button size="sm" variant="ghost" disabled={pending} onClick={() => setDeleting(false)}>
            취소
          </Button>
        </div>
      )}
    </li>
  );
}
