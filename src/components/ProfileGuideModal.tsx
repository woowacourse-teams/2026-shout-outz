import { ArrowRight, X } from 'lucide-react'
import { useEffect, useState } from 'react'

interface ProfileGuideModalProps {
  open: boolean
  onClose: () => void
  onHideToday: () => void
  onGoToProfile: () => void
}

export function ProfileGuideModal({ open, onClose, onHideToday, onGoToProfile }: ProfileGuideModalProps) {
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
    <div className="profile-guide-modal" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) dismiss() }}>
      <section className="profile-guide-modal__dialog" role="dialog" aria-modal="true" aria-labelledby="profile-guide-title">
        <button type="button" className="profile-guide-modal__close" onClick={dismiss} aria-label="안내 닫기"><X size={18} /></button>
        <div className="profile-guide-modal__art" role="img" aria-label="프로필 설정 화면 예시">
          <svg viewBox="0 0 480 230" aria-hidden="true">
            <rect width="480" height="230" rx="18" fill="#272729" />
            <rect x="28" y="26" width="424" height="178" rx="12" fill="#fff" />
            <rect x="28" y="26" width="424" height="34" rx="12" fill="#f5f5f7" />
            <circle cx="52" cy="43" r="5" fill="#d2d2d7" />
            <circle cx="68" cy="43" r="5" fill="#d2d2d7" />
            <circle cx="84" cy="43" r="5" fill="#d2d2d7" />
            <circle cx="99" cy="106" r="28" fill="#d9e6ff" />
            <circle cx="99" cy="99" r="9" fill="#0066cc" />
            <path d="M82 125c4-10 11-15 17-15s13 5 17 15" fill="#0066cc" />
            <rect x="150" y="84" width="122" height="11" rx="5.5" fill="#1d1d1f" />
            <rect x="150" y="106" width="88" height="8" rx="4" fill="#a1a1a6" />
            <rect x="62" y="154" width="232" height="8" rx="4" fill="#e5e5e7" />
            <rect x="62" y="170" width="184" height="8" rx="4" fill="#e5e5e7" />
            <rect x="334" y="91" width="82" height="34" rx="17" fill="#0066cc" />
            <path d="M356 108h34m-8-8 8 8-8 8" fill="none" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
            <circle cx="415" cy="169" r="18" fill="#e5f0ff" />
            <path d="m407 169 5 5 10-11" fill="none" stroke="#0066cc" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </div>
        <div className="profile-guide-modal__copy">
          <span className="section-kicker">프로필 안내</span>
          <h2 id="profile-guide-title">앱을 올리기 전에<br /><em>프로필을 완성해보세요.</em></h2>
          <p>프로필을 설정하면 서비스에 제작자 이름과 소개가 함께 표시됩니다.</p>
        </div>
        <label className="profile-guide-modal__check"><input type="checkbox" checked={hideToday} onChange={(event) => setHideToday(event.target.checked)} /> <span>오늘 하루 동안 보지 않기</span></label>
        <div className="profile-guide-modal__actions">
          <button type="button" className="button button--secondary" onClick={dismiss}>나중에 하기</button>
          <button type="button" className="button button--primary" onClick={() => { dismiss(); onGoToProfile() }}>프로필 설정하기 <ArrowRight size={16} /></button>
        </div>
      </section>
    </div>
  )
}
