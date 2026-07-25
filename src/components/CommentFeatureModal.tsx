import { MessageCircle, X } from 'lucide-react'
import { useEffect, useState } from 'react'

interface CommentFeatureModalProps {
  open: boolean
  onClose: (hideForToday: boolean) => void
}

export function CommentFeatureModal({ open, onClose }: CommentFeatureModalProps) {
  const [hideToday, setHideToday] = useState(false)

  useEffect(() => {
    if (open) setHideToday(false)
  }, [open])

  if (!open) return null

  const dismiss = () => onClose(hideToday)

  return (
    <div className="feature-notice-modal" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) dismiss() }}>
      <section className="feature-notice-modal__dialog" role="dialog" aria-modal="true" aria-labelledby="comment-feature-title">
        <button type="button" className="feature-notice-modal__close" onClick={dismiss} aria-label="안내 닫기"><X size={18} /></button>
        <div className="feature-notice-modal__art" aria-hidden="true">
          <div className="feature-notice-modal__bubble feature-notice-modal__bubble--primary"><MessageCircle size={25} /></div>
          <div className="feature-notice-modal__bubble feature-notice-modal__bubble--secondary"><MessageCircle size={18} /></div>
          <span className="feature-notice-modal__line feature-notice-modal__line--short" />
          <span className="feature-notice-modal__line feature-notice-modal__line--long" />
        </div>
        <div className="feature-notice-modal__copy">
          <span className="section-kicker">새로운 기능</span>
          <h2 id="comment-feature-title">서비스에<br /><em>댓글 기능이 추가됐어요.</em></h2>
          <p>오류 제보나 기능 개선 의견을 댓글로 남기고, 댓글을 눌러 다른 크루들과 대화를 이어갈 수 있습니다. <strong>욕설, 비방, 명예훼손, 차별적 표현과 개인정보 등 민감한 정보는 남기지 마세요. 문제되는 댓글은 삭제될 수 있습니다.</strong></p>
        </div>
        <label className="feature-notice-modal__check"><input type="checkbox" checked={hideToday} onChange={(event) => setHideToday(event.target.checked)} /> <span>오늘 하루 동안 보지 않기</span></label>
        <div className="feature-notice-modal__actions">
          <button type="button" className="button button--primary" onClick={dismiss}>확인했어요</button>
        </div>
      </section>
    </div>
  )
}
