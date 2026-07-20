import { ArrowRight, X } from 'lucide-react'
import { useEffect, useState } from 'react'

interface MarkdownGuideModalProps {
  open: boolean
  onClose: () => void
  onHideToday: () => void
}

export function MarkdownGuideModal({ open, onClose, onHideToday }: MarkdownGuideModalProps) {
  const [hideToday, setHideToday] = useState(false)

  useEffect(() => {
    if (open) setHideToday(false)
  }, [open])

  if (!open) return null

  const dismiss = () => {
    if (hideToday) onHideToday()
    else onClose()
  }

  return (
    <div className="profile-guide-modal markdown-guide-modal" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) dismiss() }}>
      <section className="profile-guide-modal__dialog markdown-guide-modal__dialog" role="dialog" aria-modal="true" aria-labelledby="markdown-guide-title">
        <button type="button" className="profile-guide-modal__close" onClick={dismiss} aria-label="안내 닫기"><X size={18} /></button>
        <div className="profile-guide-modal__art markdown-guide-modal__art" role="img" aria-label="마크다운 입력과 서비스 설명 미리보기 예시">
          <svg viewBox="0 0 480 230" aria-hidden="true">
            <rect width="480" height="230" rx="18" fill="#272729" />
            <rect x="24" y="24" width="190" height="182" rx="12" fill="#1d1d1f" stroke="#4a4a4d" />
            <rect x="40" y="42" width="70" height="8" rx="4" fill="#70a8de" />
            <rect x="40" y="64" width="112" height="8" rx="4" fill="#c7c7cc" />
            <rect x="40" y="93" width="122" height="7" rx="3.5" fill="#82d3bd" />
            <rect x="40" y="111" width="104" height="7" rx="3.5" fill="#a1a1a6" />
            <rect x="40" y="129" width="136" height="7" rx="3.5" fill="#a1a1a6" />
            <rect x="40" y="166" width="92" height="7" rx="3.5" fill="#e4c4ae" />
            <path d="M231 114h25m-8-8 8 8-8 8" fill="none" stroke="#70a8de" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
            <rect x="266" y="24" width="190" height="182" rx="12" fill="#fff" />
            <rect x="284" y="43" width="73" height="10" rx="5" fill="#0066cc" />
            <rect x="284" y="67" width="132" height="13" rx="6.5" fill="#1d1d1f" />
            <rect x="284" y="94" width="138" height="7" rx="3.5" fill="#a1a1a6" />
            <rect x="284" y="111" width="112" height="7" rx="3.5" fill="#a1a1a6" />
            <circle cx="291" cy="143" r="3" fill="#0066cc" />
            <rect x="303" y="139" width="98" height="7" rx="3.5" fill="#c7c7cc" />
            <circle cx="291" cy="163" r="3" fill="#0066cc" />
            <rect x="303" y="159" width="76" height="7" rx="3.5" fill="#c7c7cc" />
            <rect x="284" y="183" width="52" height="5" rx="2.5" fill="#e5f0ff" />
          </svg>
        </div>
        <div className="profile-guide-modal__copy markdown-guide-modal__copy">
          <span className="section-kicker">새로운 기능</span>
          <h2 id="markdown-guide-title">서비스 설명을<br /><em>마크다운으로 정리해보세요.</em></h2>
          <p>제목, 목록, 강조 표시를 넣어 서비스 소개를 읽기 쉽게 작성할 수 있습니다.</p>
        </div>
        <label className="profile-guide-modal__check"><input type="checkbox" checked={hideToday} onChange={(event) => setHideToday(event.target.checked)} /> <span>오늘 하루 동안 보지 않기</span></label>
        <div className="profile-guide-modal__actions">
          <button type="button" className="button button--secondary" onClick={dismiss}>나중에 보기</button>
          <button type="button" className="button button--primary" onClick={dismiss}>알겠어요 <ArrowRight size={16} /></button>
        </div>
      </section>
    </div>
  )
}
