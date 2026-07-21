import { ArrowRight, X } from 'lucide-react'
import { useEffect, useState } from 'react'

interface CategoryGuideModalProps {
  open: boolean
  onClose: () => void
  onHideToday: () => void
  onGoToMyServices: () => void
}

const addedCategories = ['여행', 'AI', '하네스', '자기개발', '개발', '디자인', '생활', '건강']

export function CategoryGuideModal({ open, onClose, onHideToday, onGoToMyServices }: CategoryGuideModalProps) {
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
    <div className="profile-guide-modal category-guide-modal" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) dismiss() }}>
      <section className="profile-guide-modal__dialog category-guide-modal__dialog" role="dialog" aria-modal="true" aria-labelledby="category-guide-title">
        <button type="button" className="profile-guide-modal__close" onClick={dismiss} aria-label="안내 닫기"><X size={18} /></button>
        <div className="profile-guide-modal__art category-guide-modal__art" role="img" aria-label="서비스 카테고리 목록 예시">
          <svg viewBox="0 0 480 230" aria-hidden="true">
            <rect width="480" height="230" rx="18" fill="#272729" />
            <rect x="26" y="24" width="428" height="182" rx="13" fill="#fff" />
            <rect x="26" y="24" width="428" height="36" rx="13" fill="#f5f5f7" />
            <circle cx="51" cy="42" r="5" fill="#d2d2d7" />
            <circle cx="67" cy="42" r="5" fill="#d2d2d7" />
            <circle cx="83" cy="42" r="5" fill="#d2d2d7" />
            <rect x="52" y="84" width="94" height="36" rx="18" fill="#0066cc" />
            <rect x="158" y="84" width="82" height="36" rx="18" fill="#e5f0ff" />
            <rect x="252" y="84" width="92" height="36" rx="18" fill="#e5f0ff" />
            <rect x="356" y="84" width="56" height="36" rx="18" fill="#e5f0ff" />
            <rect x="52" y="138" width="78" height="36" rx="18" fill="#e5f0ff" />
            <rect x="142" y="138" width="94" height="36" rx="18" fill="#e5f0ff" />
            <rect x="248" y="138" width="70" height="36" rx="18" fill="#e5f0ff" />
            <rect x="330" y="138" width="82" height="36" rx="18" fill="#e5f0ff" />
            <path d="M97 102h32m-8-8 8 8-8 8" fill="none" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
            <circle cx="416" cy="184" r="13" fill="#d9e6ff" />
            <path d="m410 184 4 4 8-9" fill="none" stroke="#0066cc" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </div>
        <div className="profile-guide-modal__copy category-guide-modal__copy">
          <span className="section-kicker">서비스 업데이트</span>
          <h2 id="category-guide-title">서비스 카테고리가<br /><em>새롭게 추가되었습니다.</em></h2>
          <p>내 서비스에 더 잘 맞는 카테고리를 골라 소개해보세요.</p>
          <div className="category-guide-modal__added">
            <strong>추가된 사항</strong>
            <div className="category-guide-modal__chips">
              {addedCategories.map((category) => <span key={category}>{category}</span>)}
            </div>
          </div>
          <p className="category-guide-modal__edit-note">지금 내 서비스 정보를 수정해보세요.</p>
        </div>
        <label className="profile-guide-modal__check"><input type="checkbox" checked={hideToday} onChange={(event) => setHideToday(event.target.checked)} /> <span>오늘 하루 동안 보지 않기</span></label>
        <div className="profile-guide-modal__actions">
          <button type="button" className="button button--secondary" onClick={dismiss}>나중에 보기</button>
          <button type="button" className="button button--primary" onClick={() => { dismiss(); onGoToMyServices() }}>내 서비스 확인하기 <ArrowRight size={16} /></button>
        </div>
      </section>
    </div>
  )
}
