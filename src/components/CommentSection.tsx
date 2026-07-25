import type { FormEvent } from 'react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { ChevronDown, MessageCircle, Send, ShieldAlert, Trash2 } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import type { AppComment, Maker } from '../types'
import { trackEvent } from '../utils/analytics'
import { createRemoteComment, deleteRemoteComment, fetchRemoteComments } from '../utils/supabaseData'
import { Avatar } from './Avatar'

const COMMENT_MAX_LENGTH = 300
const COMMENT_MAX_LINES = 4

function countCommentLines(value: string) {
  return value ? value.split(/\r?\n/).length : 0
}

function limitCommentInput(value: string) {
  return value.replace(/\r\n/g, '\n').split('\n').slice(0, COMMENT_MAX_LINES).join('\n').slice(0, COMMENT_MAX_LENGTH)
}

function formatCommentDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric' }).format(new Date(value))
}

function CommentComposer({ author, value, onChange, onSubmit, isSubmitting, isReply = false, placeholder, submitLabel }: {
  author: Maker
  value: string
  onChange: (value: string) => void
  onSubmit: () => Promise<void>
  isSubmitting: boolean
  isReply?: boolean
  placeholder: string
  submitLabel: string
}) {
  const handleChange = (nextValue: string) => onChange(limitCommentInput(nextValue))

  return (
    <form className="comment-form" onSubmit={(event: FormEvent<HTMLFormElement>) => { event.preventDefault(); void onSubmit() }}>
      <div className="comment-form__author"><Avatar maker={author} size="small" /><strong>{author.name}</strong></div>
      <textarea value={value} onChange={(event) => handleChange(event.target.value)} maxLength={COMMENT_MAX_LENGTH} placeholder={placeholder} aria-label={isReply ? '답글 내용' : '댓글 내용'} rows={4} disabled={isSubmitting} />
      <p className="comment-form__notice"><ShieldAlert size={13} /> <strong>욕설, 비방, 명예훼손, 차별적 표현과 개인정보 등 민감한 정보는 남기지 마세요. 문제되는 댓글은 삭제될 수 있습니다.</strong></p>
      <div className="comment-form__footer">
        <span>{value.length}/{COMMENT_MAX_LENGTH}자 · {countCommentLines(value)}/{COMMENT_MAX_LINES}줄</span>
        <button type="submit" className="button button--primary button--small" disabled={isSubmitting || !value.trim()}>
          <Send size={14} /> {isSubmitting ? '등록 중...' : submitLabel}
        </button>
      </div>
    </form>
  )
}

export function CommentSection({ appId, currentUser }: { appId: string; currentUser: Maker | null }) {
  const navigate = useNavigate()
  const commentsListRef = useRef<HTMLDivElement>(null)
  const [comments, setComments] = useState<AppComment[]>([])
  const [content, setContent] = useState('')
  const [replyContent, setReplyContent] = useState('')
  const [expandedCommentId, setExpandedCommentId] = useState<string | null>(null)
  const [replyingTo, setReplyingTo] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [deletingCommentId, setDeletingCommentId] = useState<string | null>(null)
  const [loadError, setLoadError] = useState('')
  const [actionError, setActionError] = useState('')

  const topLevelComments = useMemo(() => comments.filter((comment) => !comment.parentId), [comments])
  const repliesByParent = useMemo(() => {
    const grouped = new Map<string, AppComment[]>()
    comments.filter((comment) => comment.parentId).forEach((comment) => {
      const parentId = comment.parentId as string
      grouped.set(parentId, [...(grouped.get(parentId) ?? []), comment])
    })
    grouped.forEach((replies, parentId) => grouped.set(parentId, replies.sort((first, second) => first.createdAt.localeCompare(second.createdAt))))
    return grouped
  }, [comments])

  useEffect(() => {
    let active = true
    setIsLoading(true)
    setLoadError('')
    setActionError('')
    void fetchRemoteComments(appId).then((nextComments) => {
      if (active) setComments(nextComments)
    }).catch(() => {
      if (active) setLoadError('댓글을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.')
    }).finally(() => {
      if (active) setIsLoading(false)
    })
    return () => {
      active = false
    }
  }, [appId])

  const goToLogin = () => {
    const returnTo = `${window.location.pathname}${window.location.search}`
    navigate(`/login?returnTo=${encodeURIComponent(returnTo)}`)
  }

  const submitComment = async (parentId: string | null) => {
    if (!currentUser) {
      goToLogin()
      return
    }
    const value = parentId ? replyContent : content
    const trimmedContent = value.trim()
    if (!trimmedContent) {
      setActionError(parentId ? '답글 내용을 입력해주세요.' : '댓글 내용을 입력해주세요.')
      return
    }
    if (trimmedContent.length > COMMENT_MAX_LENGTH || countCommentLines(trimmedContent) > COMMENT_MAX_LINES) {
      setActionError(`댓글은 ${COMMENT_MAX_LENGTH}자, ${COMMENT_MAX_LINES}줄 이내로 작성해주세요.`)
      return
    }

    setIsSubmitting(true)
    setActionError('')
    try {
      const comment = await createRemoteComment(appId, currentUser.id, trimmedContent, currentUser, parentId)
      trackEvent(parentId ? 'reply_created' : 'comment_created', { app_id: appId })
      setComments((current) => parentId ? [...current, comment] : [comment, ...current])
      if (parentId) {
        setReplyContent('')
        setReplyingTo(null)
        setExpandedCommentId(parentId)
      } else {
        setContent('')
      }
    } catch {
      setActionError(parentId ? '답글을 등록하지 못했습니다. 잠시 후 다시 시도해주세요.' : '댓글을 등록하지 못했습니다. 잠시 후 다시 시도해주세요.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const toggleThread = (commentId: string) => {
    setExpandedCommentId((current) => current === commentId ? null : commentId)
    setReplyingTo(null)
    setReplyContent('')
    setActionError('')
  }

  const toggleReplyForm = (commentId: string) => {
    setExpandedCommentId(commentId)
    setReplyingTo((current) => current === commentId ? null : commentId)
    setReplyContent('')
    setActionError('')
  }

  const scrollComments = (direction: number) => {
    const list = commentsListRef.current
    if (!list) return
    list.scrollBy({ left: direction * Math.max(list.clientWidth * 0.82, 280), behavior: 'smooth' })
  }

  const removeComment = async (comment: AppComment) => {
    if (!currentUser || currentUser.id !== comment.userId || deletingCommentId) return
    if (!window.confirm(comment.parentId ? '이 답글을 삭제할까요?' : '이 댓글을 삭제할까요?')) return
    setDeletingCommentId(comment.id)
    setActionError('')
    try {
      await deleteRemoteComment(comment.id, currentUser.id)
      setComments((current) => current.filter((item) => item.id !== comment.id && item.parentId !== comment.id))
      if (!comment.parentId) {
        setExpandedCommentId(null)
        setReplyingTo(null)
      }
    } catch {
      setActionError(comment.parentId ? '답글을 삭제하지 못했습니다. 잠시 후 다시 시도해주세요.' : '댓글을 삭제하지 못했습니다. 잠시 후 다시 시도해주세요.')
    } finally {
      setDeletingCommentId(null)
    }
  }

  return (
    <section className="detail-comments" aria-labelledby="comments-title">
      <div className="content-container">
        <div className="detail-comments__heading">
          <span className="section-kicker"><MessageCircle size={14} /> 크루 댓글</span>
          <h2 id="comments-title">댓글 <small>{comments.length}개</small></h2>
        </div>
        <div className="comments-panel">
          {currentUser ? <CommentComposer author={currentUser} value={content} onChange={setContent} onSubmit={() => submitComment(null)} isSubmitting={isSubmitting} placeholder="서비스에 대한 댓글을 남겨보세요." submitLabel="댓글 남기기" /> : <div className="comments-login-prompt"><div><strong>로그인하고 댓글을 남겨보세요.</strong><p>서비스를 사용한 크루의 의견을 기다리고 있습니다.</p></div><button type="button" className="button button--secondary button--small" onClick={goToLogin}>로그인</button></div>}
          {loadError ? <p className="comments-message comments-message--error" role="alert">{loadError}</p> : null}
          {actionError ? <p className="comments-message comments-message--error" role="alert">{actionError}</p> : null}

          <div className="comments-carousel">
            {topLevelComments.length > 2 ? <>
              <button type="button" className="comments-carousel__button comments-carousel__button--previous" onClick={() => scrollComments(-1)} aria-label="이전 댓글"><ChevronDown size={18} className="comments-carousel__icon--previous" /></button>
              <button type="button" className="comments-carousel__button comments-carousel__button--next" onClick={() => scrollComments(1)} aria-label="다음 댓글"><ChevronDown size={18} className="comments-carousel__icon--next" /></button>
            </> : null}
            <div className="comments-list" ref={commentsListRef}>
              {isLoading ? <p className="comments-empty">댓글을 불러오고 있습니다.</p> : null}
              {!isLoading && !loadError && topLevelComments.length === 0 ? <p className="comments-empty">아직 댓글이 없습니다.</p> : null}
              {!isLoading ? topLevelComments.map((comment) => {
                const replies = repliesByParent.get(comment.id) ?? []
                const isExpanded = expandedCommentId === comment.id
                return (
                  <article className={`comment-card ${isExpanded ? 'is-expanded' : ''}`} key={comment.id}>
                    <button type="button" className="comment-card__toggle" onClick={() => toggleThread(comment.id)} aria-expanded={isExpanded}>
                      <div className="comment-card__header">
                        <div className="comment-card__author"><Avatar maker={comment.author} size="small" /><span><strong>{comment.author.name}</strong><small>{formatCommentDate(comment.createdAt)}</small></span></div>
                      </div>
                      <p>{comment.content}</p>
                    </button>
                    <div className={`comment-card__actions ${currentUser?.id === comment.userId ? 'has-delete' : ''}`}>
                      {currentUser?.id === comment.userId ? <button type="button" className="comment-card__delete" onClick={() => void removeComment(comment)} disabled={deletingCommentId === comment.id} aria-label="댓글 삭제"><Trash2 size={14} /> 삭제</button> : null}
                      <button type="button" className="comment-card__reply" onClick={() => toggleThread(comment.id)} aria-expanded={isExpanded}>{isExpanded ? '접기' : replies.length > 0 ? `답글 ${replies.length}개` : '답글 달기'} <ChevronDown size={15} /></button>
                    </div>
                    {isExpanded ? <div className="comment-thread">
                      {replies.length > 0 ? <div className="comment-thread__replies">{replies.map((reply) => <article className="comment-reply" key={reply.id}><div className="comment-reply__header"><div className="comment-card__author"><Avatar maker={reply.author} size="small" /><span><strong>{reply.author.name}</strong><small>{formatCommentDate(reply.createdAt)}</small></span></div>{currentUser?.id === reply.userId ? <button type="button" className="comment-card__delete" onClick={() => void removeComment(reply)} disabled={deletingCommentId === reply.id} aria-label="답글 삭제"><Trash2 size={14} /> 삭제</button> : null}</div><p>{reply.content}</p></article>)}</div> : <p className="comment-thread__empty">아직 답글이 없습니다.</p>}
                      {replyingTo === comment.id && currentUser ? <CommentComposer author={currentUser} value={replyContent} onChange={setReplyContent} onSubmit={() => submitComment(comment.id)} isSubmitting={isSubmitting} isReply placeholder="이 댓글에 답글을 남겨보세요." submitLabel="답글 남기기" /> : <button type="button" className="comment-thread__reply" onClick={() => currentUser ? toggleReplyForm(comment.id) : goToLogin()}>{currentUser ? '답글 달기' : '로그인하고 답글 달기'}</button>}
                    </div> : null}
                  </article>
                )
              }) : null}
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
