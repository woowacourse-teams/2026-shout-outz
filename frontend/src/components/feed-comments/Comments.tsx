import { Component, Suspense, useState, type ReactNode } from 'react';
import { Link } from '@tanstack/react-router';
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
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { LinkifiedText } from '@/components/LinkifiedText';
import { LinkPreview } from '@/components/feeds/LinkPreview';
import { formatDateTime } from '@/utils/date';
import { getApiErrorMessage } from '@/utils/error';
import { getGithubLoginUrl } from '@/utils/auth';
import { analytics, toPathPattern } from '@/utils/analytics';
import {
  IconCornerDownRight,
  IconPencil,
  IconThumbUp,
  IconThumbUpFilled,
  IconTrash,
} from '@tabler/icons-react';
import { setFeedCommentAgree } from '@/apis/reaction';
import { useRequireAuthentication } from '@/hooks/useRequireAuthentication';
import type { FeedType } from '@/types/feed';

export function Comments({
  feedId,
  feedType = 'POST',
  feedAuthorId = null,
}: {
  feedId: number;
  feedType?: FeedType;
  feedAuthorId?: number | null;
}) {
  return (
    <QueryErrorResetBoundary>
      {({ reset }) => (
        <SessionBoundary
          reset={reset}
          guest={<GuestComments feedId={feedId} feedType={feedType} feedAuthorId={feedAuthorId} />}
        >
          <Suspense
            fallback={
              <p role="status" className="p-6 text-gray-500">
                불러오는 중…
              </p>
            }
          >
            <CommentsWithSession feedId={feedId} feedType={feedType} feedAuthorId={feedAuthorId} />
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

function GuestComments({
  feedId,
  feedType,
  feedAuthorId,
}: {
  feedId: number;
  feedType: FeedType;
  feedAuthorId: number | null;
}) {
  return (
    <AsyncBoundary key={`guest-${feedId}`}>
      <CommentList
        feedId={feedId}
        feedType={feedType}
        feedAuthorId={feedAuthorId}
        viewer={null}
        sessionReady={false}
      />
    </AsyncBoundary>
  );
}

function CommentsWithSession({
  feedId,
  feedType,
  feedAuthorId,
}: {
  feedId: number;
  feedType: FeedType;
  feedAuthorId: number | null;
}) {
  const session = useSuspenseQuery(sessionQuery);
  const viewer = session.data.status === 'AUTHENTICATED' ? (session.data.userId ?? null) : null;

  return (
    <AsyncBoundary key={viewer ?? 'guest'}>
      <CommentList
        feedId={feedId}
        feedType={feedType}
        feedAuthorId={feedAuthorId}
        viewer={viewer}
        sessionReady={!session.isFetching && !session.isError}
      />
    </AsyncBoundary>
  );
}
function CommentList({
  feedId,
  feedType,
  feedAuthorId,
  viewer,
  sessionReady,
}: {
  feedId: number;
  feedType: FeedType;
  feedAuthorId: number | null;
  viewer: number | null;
  sessionReady: boolean;
}) {
  const query = useSuspenseInfiniteQuery(commentsQuery(feedId, viewer));
  const client = useQueryClient();
  const mutation = useMutation(commentMutation(feedId));
  const [content, setContent] = useState('');
  const [isAnonymous, setIsAnonymous] = useState(false);
  const [message, setMessage] = useState('');
  const [failure, setFailure] = useState('');
  const [refreshError, setRefreshError] = useState<unknown | null>(null);
  const isQuestion = feedType === 'QUESTION';
  const itemLabel = isQuestion ? '답변' : '댓글';
  const items = [
    ...new Map(
      query.data.pages.flatMap((page) => page.data).map((item) => [item.id, item]),
    ).values(),
  ];
  const repliesByParent = new Map<number, FeedComment[]>();
  const roots = items.filter((item) => item.parentId == null);
  for (const item of items) {
    if (item.parentId == null) continue;
    const replies = repliesByParent.get(item.parentId) ?? [];
    replies.push(item);
    repliesByParent.set(item.parentId, replies);
  }
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
    const changedItem = items.find((item) => item.id === input.commentId);
    const changedLabel =
      (input.method === 'post' ? input.parentId : changedItem?.parentId) == null
        ? itemLabel
        : '답글';
    setMessage(
      input.method === 'delete'
        ? `${changedLabel}을 삭제했습니다.`
        : `${changedLabel}을 저장했습니다.`,
    );
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
              void change({ method: 'post', content, isAnonymous }, () => {
                setContent('');
                setIsAnonymous(false);
              });
          }}
          className="space-y-3"
        >
          <label
            className="block text-xs leading-5 font-semibold text-gray-800"
            htmlFor={`new-comment-${feedId}`}
          >
            <span className="inline-flex items-center gap-1.5">{itemLabel} 남기기</span>
          </label>
          <textarea
            id={`new-comment-${feedId}`}
            rows={5}
            maxLength={500}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            disabled={mutation.isPending}
            placeholder={
              isQuestion
                ? '질문에 도움이 될 답변을 작성해 주세요.'
                : '나의 경험과 도움이 될 만한 이야기를 적어주세요.'
            }
            className="focus-visible:border-primary-500 focus-visible:ring-primary-500 w-full resize-y rounded-lg border border-gray-200 bg-transparent p-4 text-sm leading-6 text-gray-900 outline-none placeholder:text-gray-400 focus-visible:ring-1"
          />
          <div className="flex flex-wrap items-center gap-3">
            <label className="flex items-center gap-2 text-xs text-gray-500">
              <input
                type="checkbox"
                checked={isAnonymous}
                onChange={(event) => setIsAnonymous(event.target.checked)}
                disabled={mutation.isPending}
                className="accent-primary-600"
              />
              익명으로 남기기
            </label>
            <span className="text-xs text-gray-400">{Array.from(content).length} / 500</span>
            <Button
              className="ml-auto"
              variant="primary"
              size="sm"
              type="submit"
              aria-label={`${itemLabel} 작성`}
              disabled={!content.trim() || mutation.isPending || !sessionReady}
            >
              {itemLabel} 등록하기
            </Button>
          </div>
        </form>
      ) : (
        <p className="rounded-lg bg-gray-50 p-3 text-sm text-gray-600">
          {itemLabel}을 작성하려면{' '}
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
      {items.length === 0 && (
        <p className="py-4 text-sm text-gray-500">{`아직 ${itemLabel}이 없습니다.`}</p>
      )}
      <ul
        aria-label={`${itemLabel} 목록`}
        className="divide-y divide-gray-100 border-t border-gray-100 pt-6"
      >
        {roots.map((root) => (
          <li key={root.id} className="min-w-0 py-7 first:pt-0 last:pb-0">
            <CommentItem
              item={root}
              label={itemLabel}
              isFeedAuthor={feedAuthorId !== null && root.author.userId === feedAuthorId}
              canEdit={
                !root.deleted &&
                sessionReady &&
                viewer !== null &&
                viewer === root.author.userId &&
                root.editable
              }
              canDelete={
                !root.deleted && sessionReady && viewer !== null && viewer === root.author.userId
              }
              canReact={sessionReady}
              canReply={!root.deleted && sessionReady && viewer !== null}
              feedId={feedId}
              pending={mutation.isPending}
              change={change}
            />
            {(repliesByParent.get(root.id)?.length ?? 0) > 0 && (
              <ul
                aria-label="답글 목록"
                className="mt-7 ml-7 divide-y divide-gray-100 border-l-2 border-blue-100 bg-gray-50/70 md:ml-8"
              >
                {repliesByParent.get(root.id)!.map((reply) => (
                  <li key={reply.id} className="relative min-w-0 px-5 py-6 md:px-6 md:py-7">
                    <span
                      aria-hidden="true"
                      className="pointer-events-none absolute top-10 -left-8 h-5 w-8 rounded-bl-xl border-b-2 border-l-2 border-blue-100"
                    />
                    <CommentItem
                      item={reply}
                      label="답글"
                      isFeedAuthor={feedAuthorId !== null && reply.author.userId === feedAuthorId}
                      canEdit={
                        !reply.deleted &&
                        sessionReady &&
                        viewer !== null &&
                        viewer === reply.author.userId &&
                        reply.editable
                      }
                      canDelete={
                        !reply.deleted &&
                        sessionReady &&
                        viewer !== null &&
                        viewer === reply.author.userId
                      }
                      canReact={sessionReady}
                      canReply={false}
                      feedId={feedId}
                      pending={mutation.isPending}
                      change={change}
                    />
                  </li>
                ))}
              </ul>
            )}
          </li>
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
                ? `추가 ${itemLabel} 다시 시도`
                : `${itemLabel} 더 보기`}
          </Button>
        </div>
      )}
    </div>
  );
}
function CommentItem({
  item,
  label,
  isFeedAuthor,
  canEdit,
  canDelete,
  canReact,
  canReply,
  feedId,
  pending,
  change,
}: {
  item: FeedComment;
  label: string;
  isFeedAuthor: boolean;
  canEdit: boolean;
  canDelete: boolean;
  canReact: boolean;
  canReply: boolean;
  feedId: number;
  pending: boolean;
  change: (input: CommentChange, done: () => void) => Promise<void>;
}) {
  const client = useQueryClient();
  const { requireAuthentication } = useRequireAuthentication();
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [replying, setReplying] = useState(false);
  const [replyContent, setReplyContent] = useState('');
  const [replyAnonymous, setReplyAnonymous] = useState(false);
  const [draft, setDraft] = useState(item.content ?? '');
  const [reactionOverride, setReactionOverride] = useState<{
    baseCount: number;
    baseAgreed: boolean;
    count: number;
    agreed: boolean;
  } | null>(null);
  const baseCount = item.agreeCount ?? 0;
  const baseAgreed = item.agreedByMe ?? false;
  const currentOverride =
    reactionOverride?.baseCount === baseCount && reactionOverride.baseAgreed === baseAgreed
      ? reactionOverride
      : null;
  const agreeCount = currentOverride?.count ?? baseCount;
  const agreed = currentOverride?.agreed ?? baseAgreed;
  const trackLabels: Record<string, string> = {
    ANDROID: 'AN',
    BACKEND: 'BE',
    FRONTEND: 'FE',
  };
  const trackLabel =
    !item.isAnonymous && item.author.handle && item.author.track
      ? trackLabels[item.author.track]
      : undefined;
  const crewInfo = item.isAnonymous
    ? null
    : [
        trackLabel,
        item.author.cohort != null ? `${item.author.cohort}기` : null,
        item.author.userType === 'WOOWACOURSE_CREW' ? '크루' : null,
      ]
        .filter(Boolean)
        .join(' ');
  const isOwnAnonymous = item.isAnonymous && item.author.handle != null;
  const authorName = item.isAnonymous && !isOwnAnonymous ? '익명' : item.author.displayName;
  const agreeMutation = useMutation({
    mutationFn: (active: boolean) => setFeedCommentAgree(feedId, item.id, active),
  });

  const toggleAgree = async () => {
    if (!requireAuthentication() || !canReact || agreeMutation.isPending) return;
    const next = !agreed;
    setReactionOverride({
      baseCount,
      baseAgreed,
      count: Math.max(0, agreeCount + (next ? 1 : -1)),
      agreed: next,
    });
    try {
      const result = await agreeMutation.mutateAsync(next);
      setReactionOverride({
        baseCount,
        baseAgreed,
        count: result.agreeCount,
        agreed: result.active,
      });
      void client.invalidateQueries({ queryKey: ['feed-comments', feedId] });
    } catch {
      setReactionOverride(null);
    }
  };

  const authorDetails = (
    <>
      <Avatar size="sm" src={item.author.avatarUrl} name={authorName ?? undefined} alt="" />
      <div className="min-w-0 flex-1">
        <div className="flex min-w-0 flex-wrap items-center gap-1.5">
          <span className="group-hover:text-primary-600 truncate text-xs leading-5 font-semibold text-gray-900">
            {authorName}
          </span>
          <CrewStatusBadge
            userType={item.author.userType}
            cohort={item.author.cohort}
            isCurrent={item.author.isCurrent}
            size="xs"
          />
          {isFeedAuthor && (
            <span className="bg-primary-50 text-primary-700 rounded-full px-1.5 text-xs font-medium">
              작성자
            </span>
          )}
          {isOwnAnonymous && (
            <span className="bg-primary-50 text-primary-700 rounded-full px-1.5 text-xs font-medium">
              익명으로 작성한 글입니다
            </span>
          )}
        </div>
        {crewInfo && <p className="mt-0.5 text-xs leading-4 text-gray-500">{crewInfo}</p>}
      </div>
    </>
  );

  return (
    <div className="min-w-0">
      <div className="flex min-w-0 items-center gap-3">
        {item.author.handle ? (
          <Link
            to="/users/$handle"
            params={{ handle: item.author.handle }}
            aria-label={`${item.author.displayName} 프로필 보기`}
            className="group focus-visible:outline-primary-600 flex min-w-0 flex-1 items-center gap-3 rounded-sm focus-visible:outline-2"
          >
            {authorDetails}
          </Link>
        ) : (
          <div className="flex min-w-0 flex-1 items-center gap-3">{authorDetails}</div>
        )}
        <time dateTime={item.createdAt} className="shrink-0 text-xs text-gray-500">
          {formatDateTime(item.createdAt)}
          {item.edited ? ' · 수정됨' : ''}
        </time>
      </div>
      {editing && canEdit ? (
        <form
          className="mt-4 space-y-3 rounded-xl border border-gray-200 bg-gray-50/50 p-4"
          onSubmit={(e) => {
            e.preventDefault();
            if (draft.trim() && !pending)
              void change({ method: 'patch', commentId: item.id, content: draft }, () =>
                setEditing(false),
              );
          }}
        >
          <label className="sr-only" htmlFor={`edit-${item.id}`}>
            {label} 수정 내용
          </label>
          <textarea
            id={`edit-${item.id}`}
            rows={3}
            maxLength={500}
            className="bg-background focus-visible:border-primary-500 focus-visible:ring-primary-500 w-full resize-y rounded-lg border border-gray-200 p-3 text-sm leading-6 text-gray-900 outline-none focus-visible:ring-1"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            disabled={pending}
          />
          <div className="flex justify-end gap-2">
            <Button size="sm" variant="ghost" disabled={pending} onClick={() => setEditing(false)}>
              취소
            </Button>
            <Button size="sm" variant="primary" type="submit" disabled={pending || !draft.trim()}>
              수정 저장
            </Button>
          </div>
        </form>
      ) : (
        <p className="mt-4 text-sm leading-6 break-words whitespace-pre-wrap text-gray-700">
          {item.deleted ? `삭제된 ${label}입니다.` : <LinkifiedText text={item.content ?? ''} />}
        </p>
      )}
      {!item.deleted && !editing && item.linkPreview?.url && (
        <div className="mt-3 max-w-md">
          <LinkPreview size="sm" {...item.linkPreview} url={item.linkPreview.url} />
        </div>
      )}
      {!item.deleted && !editing && !deleting && (
        <div className="mt-4 flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-1">
            <Button
              size="sm"
              variant="ghost"
              className={`gap-1.5 rounded-full px-2.5 ${agreed ? 'bg-primary-50 text-primary-600 hover:bg-primary-100' : 'text-gray-500 hover:text-gray-800'}`}
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
              <span>
                공감 <span aria-label="공감 수">{agreeCount}</span>
              </span>
            </Button>
            {canReply && !replying && (
              <Button
                size="sm"
                variant="ghost"
                className="gap-1.5 rounded-full px-2.5 text-gray-500 hover:text-gray-800"
                disabled={pending}
                aria-label="답글 달기"
                onClick={() => setReplying(true)}
              >
                <IconCornerDownRight className="size-4" aria-hidden="true" />
                답글
              </Button>
            )}
          </div>
          {(canEdit || canDelete) && (
            <div className="ml-auto flex items-center gap-1">
              {canEdit && (
                <Button
                  size="sm"
                  variant="ghost"
                  className="gap-1 rounded-full px-2.5 text-gray-500 hover:text-gray-800"
                  disabled={pending}
                  onClick={() => {
                    setDraft(item.content ?? '');
                    setEditing(true);
                  }}
                >
                  <IconPencil className="size-4" aria-hidden="true" />
                  수정
                </Button>
              )}
              {canDelete && (
                <Button
                  size="sm"
                  variant="ghost"
                  className="gap-1 rounded-full px-2.5 text-gray-500 hover:bg-red-50 hover:text-red-600"
                  disabled={pending}
                  onClick={() => setDeleting(true)}
                >
                  <IconTrash className="size-4" aria-hidden="true" />
                  삭제
                </Button>
              )}
            </div>
          )}
        </div>
      )}
      {replying && canReply && (
        <form
          className="mt-4 space-y-3 rounded-xl border border-gray-200 bg-gray-50/50 p-4"
          onSubmit={(event) => {
            event.preventDefault();
            if (replyContent.trim() && !pending)
              void change(
                {
                  method: 'post',
                  content: replyContent,
                  parentId: item.id,
                  isAnonymous: replyAnonymous,
                },
                () => {
                  setReplyContent('');
                  setReplyAnonymous(false);
                  setReplying(false);
                },
              );
          }}
        >
          <label className="sr-only" htmlFor={`reply-${item.id}`}>
            답글 내용
          </label>
          <textarea
            id={`reply-${item.id}`}
            rows={3}
            maxLength={500}
            value={replyContent}
            onChange={(event) => setReplyContent(event.target.value)}
            disabled={pending}
            placeholder="답글을 작성해 주세요."
            className="bg-background focus-visible:border-primary-500 focus-visible:ring-primary-500 w-full resize-y rounded-lg border border-gray-200 p-3 text-sm leading-6 text-gray-900 outline-none focus-visible:ring-1"
          />
          <div className="flex flex-wrap items-center gap-3">
            <label className="flex items-center gap-2 text-xs text-gray-500">
              <input
                type="checkbox"
                checked={replyAnonymous}
                onChange={(event) => setReplyAnonymous(event.target.checked)}
                disabled={pending}
                className="accent-primary-600"
              />
              익명으로 남기기
            </label>
            <span className="text-xs text-gray-400">{Array.from(replyContent).length} / 500</span>
            <div className="ml-auto flex items-center gap-2">
              <Button
                size="sm"
                variant="ghost"
                disabled={pending}
                onClick={() => setReplying(false)}
              >
                취소
              </Button>
              <Button
                size="sm"
                variant="primary"
                type="submit"
                disabled={pending || !replyContent.trim()}
              >
                답글 등록
              </Button>
            </div>
          </div>
        </form>
      )}
      {deleting && canDelete && (
        <div
          role="group"
          aria-label={`${label} 삭제 확인`}
          className="mt-4 rounded-xl border border-red-100 bg-red-50/50 p-4"
        >
          <p className="text-sm text-gray-800">{`이 ${label}을 삭제할까요?`}</p>
          <div className="mt-3 flex justify-end gap-2">
            <Button size="sm" variant="ghost" disabled={pending} onClick={() => setDeleting(false)}>
              취소
            </Button>
            <Button
              size="sm"
              variant="primary"
              className="bg-red-600 hover:bg-red-700 disabled:hover:bg-red-600"
              disabled={pending}
              onClick={() =>
                void change({ method: 'delete', commentId: item.id }, () => setDeleting(false))
              }
            >
              삭제 확인
            </Button>
          </div>
        </div>
      )}
      {agreeMutation.isError && (
        <p role="alert" className="text-xs text-red-600">
          {getApiErrorMessage(agreeMutation.error)}
        </p>
      )}
    </div>
  );
}
