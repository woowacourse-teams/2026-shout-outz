import { Component, Suspense, useEffect, useState, type ReactNode } from 'react';
import {
  QueryErrorResetBoundary,
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
import { CrewStatusBadge } from '@/components/users/CrewStatusBadge';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { formatRelativeTime } from '@/utils/date';
import { getApiErrorMessage } from '@/utils/error';
import { getGithubLoginUrl } from '@/utils/auth';
import { analytics, toPathPattern } from '@/utils/analytics';
import { IconThumbUp, IconThumbUpFilled } from '@tabler/icons-react';
import { setFeedCommentAgree } from '@/apis/reaction';
import { useRequireAuthentication } from '@/hooks/useRequireAuthentication';

export function Comments({ feedId }: { feedId: number }) {
  return (
    <QueryErrorResetBoundary>
      {({ reset }) => (
        <SessionBoundary reset={reset} guest={<GuestComments feedId={feedId} />}>
          <Suspense
            fallback={
              <p role="status" className="p-6 text-gray-500">
                불러오는 중…
              </p>
            }
          >
            <CommentsWithSession feedId={feedId} />
          </Suspense>
        </SessionBoundary>
      )}
    </QueryErrorResetBoundary>
  );
}

class SessionBoundary extends Component<
  { children: ReactNode; guest: ReactNode; reset: () => void },
  { error: unknown | null }
> {
  state: { error: unknown | null } = { error: null };

  static getDerivedStateFromError(error: unknown) {
    return { error };
  }

  render() {
    if (this.state.error === null) return this.props.children;

    return (
      <div>
        <div role="alert" className="mb-3 text-sm text-gray-600">
          {getApiErrorMessage(this.state.error)}{' '}
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              this.props.reset();
              this.setState({ error: null });
            }}
          >
            로그인 상태 다시 확인
          </Button>
        </div>
        {this.props.guest}
      </div>
    );
  }
}

function GuestComments({ feedId }: { feedId: number }) {
  return (
    <AsyncBoundary key={`guest-${feedId}`}>
      <CommentList feedId={feedId} viewer={null} sessionReady={false} />
    </AsyncBoundary>
  );
}

function CommentsWithSession({ feedId }: { feedId: number }) {
  const session = useSuspenseQuery(sessionQuery);
  const viewer = session.data.status === 'AUTHENTICATED' ? (session.data.userId ?? null) : null;

  return (
    <AsyncBoundary key={viewer ?? 'guest'}>
      <CommentList
        feedId={feedId}
        viewer={viewer}
        sessionReady={!session.isFetching && !session.isError}
      />
    </AsyncBoundary>
  );
}
function CommentList({
  feedId,
  viewer,
  sessionReady,
}: {
  feedId: number;
  viewer: number | null;
  sessionReady: boolean;
}) {
  const query = useSuspenseInfiniteQuery(commentsQuery(feedId, viewer));
  const client = useQueryClient();
  const mutation = useMutation(commentMutation(feedId));
  const [content, setContent] = useState('');
  const [message, setMessage] = useState('');
  const [failure, setFailure] = useState('');
  const [refreshError, setRefreshError] = useState<unknown | null>(null);
  const items = [
    ...new Map(
      query.data.pages.flatMap((page) => page.data).map((item) => [item.id, item]),
    ).values(),
  ];
  async function change(input: CommentChange, onSuccess: () => void) {
    setFailure('');
    setMessage('');
    try {
      await mutation.mutateAsync(input);
      if (input.method === 'post') analytics.track({ name: 'comment_submitted' });
    } catch (error) {
      setFailure(getApiErrorMessage(error));
      return;
    }
    onSuccess();
    setMessage(input.method === 'delete' ? '댓글을 삭제했습니다.' : '댓글을 저장했습니다.');
    try {
      await client.invalidateQueries(
        { queryKey: ['feed-comments', feedId] },
        { throwOnError: true },
      );
      if (input.method === 'post' || input.method === 'delete') {
        void client.invalidateQueries({ queryKey: ['feed', feedId] });
        void client.invalidateQueries({ queryKey: ['feeds'] });
      }
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
          <label className="sr-only" htmlFor={`new-comment-${feedId}`}>
            댓글 남기기
          </label>
          <input
            id={`new-comment-${feedId}`}
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
            href={getGithubLoginUrl()}
            onClick={() =>
              analytics.track({
                name: 'login_started',
                from: toPathPattern(window.location.pathname),
              })
            }
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
            canEdit={
              !item.deleted && sessionReady && viewer === item.author.userId && item.editable
            }
            canDelete={!item.deleted && sessionReady && viewer === item.author.userId}
            canReact={sessionReady}
            feedId={feedId}
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
  canReact,
  feedId,
  pending,
  change,
}: {
  item: FeedComment;
  canEdit: boolean;
  canDelete: boolean;
  canReact: boolean;
  feedId: number;
  pending: boolean;
  change: (input: CommentChange, done: () => void) => Promise<void>;
}) {
  const client = useQueryClient();
  const { requireAuthentication } = useRequireAuthentication();
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [draft, setDraft] = useState(item.content ?? '');
  const [agreeCount, setAgreeCount] = useState(item.agreeCount ?? 0);
  const [agreed, setAgreed] = useState(item.agreedByMe ?? false);
  const trackLabels: Record<string, string> = {
    ANDROID: 'AN',
    BACKEND: 'BE',
    FRONTEND: 'FE',
  };
  const trackLabel = item.author.track ? trackLabels[item.author.track] : undefined;
  const crewInfo = [
    trackLabel,
    item.author.cohort == null ? null : `${item.author.cohort}기`,
    item.author.userType === 'WOOWACOURSE_CREW' ? '크루' : null,
  ]
    .filter(Boolean)
    .join(' ');
  const agreeMutation = useMutation({
    mutationFn: (active: boolean) => setFeedCommentAgree(feedId, item.id, active),
  });

  useEffect(() => {
    setAgreeCount(item.agreeCount ?? 0);
    setAgreed(item.agreedByMe ?? false);
  }, [item.agreeCount, item.agreedByMe]);

  const toggleAgree = async () => {
    if (!requireAuthentication() || !canReact || agreeMutation.isPending) return;
    const previous = { agreeCount, agreed };
    const next = !agreed;
    setAgreed(next);
    setAgreeCount(Math.max(0, agreeCount + (next ? 1 : -1)));
    try {
      const result = await agreeMutation.mutateAsync(next);
      setAgreeCount(result.agreeCount);
      setAgreed(result.active);
      void client.invalidateQueries({ queryKey: ['feed-comments', feedId] });
    } catch {
      setAgreeCount(previous.agreeCount);
      setAgreed(previous.agreed);
    }
  };

  return (
    <li className="min-w-0 py-4 first:pt-0 last:pb-0">
      <div className="flex min-w-0 items-center gap-3">
        <Avatar size="sm" src={item.author.avatarUrl ?? undefined} alt="" />
        <div className="min-w-0 flex-1">
          <div className="flex min-w-0 items-center gap-1.5">
            <span className="truncate text-sm font-semibold text-gray-900">
              {item.author.displayName}
            </span>
            <CrewStatusBadge
              userType={item.author.userType}
              cohort={item.author.cohort}
              size="xs"
            />
          </div>
          {crewInfo && <p className="mt-0.5 text-sm text-gray-500">{crewInfo}</p>}
        </div>
        <time dateTime={item.createdAt} className="shrink-0 text-sm text-gray-500">
          {formatRelativeTime(item.createdAt)}
          {item.edited ? ' · 수정됨' : ''}
        </time>
      </div>
      {item.parentId !== null && (
        <p className="mt-3 text-xs text-gray-500">댓글 #{item.parentId}에 대한 답글</p>
      )}
      {editing && canEdit ? (
        <form
          className="mt-3 space-y-2"
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
        <p className="mt-4 text-sm leading-6 break-words whitespace-pre-wrap text-gray-700">
          {item.deleted ? '삭제된 댓글입니다.' : item.content}
        </p>
      )}
      <div className="mt-3 flex items-center gap-1">
        {!item.deleted && (
          <Button
            size="sm"
            variant="ghost"
            className={`gap-1 px-2 ${agreed ? 'text-primary-600' : ''}`}
            aria-label={agreed ? '공감 취소' : '공감'}
            aria-pressed={agreed}
            disabled={agreeMutation.isPending}
            onClick={() => void toggleAgree()}
          >
            {agreed ? (
              <IconThumbUpFilled className="size-4" aria-hidden="true" />
            ) : (
              <IconThumbUp className="size-4" aria-hidden="true" />
            )}
            <span aria-label="공감 수">{agreeCount}</span>
          </Button>
        )}
        {canEdit && !editing && (
          <Button
            size="sm"
            variant="ghost"
            disabled={pending}
            onClick={() => {
              setDraft(item.content ?? '');
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
      {agreeMutation.isError && (
        <p role="alert" className="text-xs text-red-600">
          {getApiErrorMessage(agreeMutation.error)}
        </p>
      )}
    </li>
  );
}
