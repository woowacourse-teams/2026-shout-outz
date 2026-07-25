import { Users, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

interface CrewFeatureModalProps {
  open: boolean
  onClose: (hideForToday: boolean) => void
}

export function CrewFeatureModal({ open, onClose }: CrewFeatureModalProps) {
  const [hideToday, setHideToday] = useState(false)

  useEffect(() => {
    if (open) setHideToday(false)
  }, [open])

  if (!open) return null

  const dismiss = () => onClose(hideToday)

  return (
    <div className="feature-notice-modal" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) dismiss() }}>
      <section className="feature-notice-modal__dialog" role="dialog" aria-modal="true" aria-labelledby="crew-feature-title">
        <button type="button" className="feature-notice-modal__close" onClick={dismiss} aria-label="안내 닫기"><X size={18} /></button>
        <div className="crew-feature-modal__image">
          <img src="/crew-directory-feature.png" alt="크루별 모아보기 화면 미리보기" />
        </div>
        <div className="feature-notice-modal__copy">
          <span className="section-kicker"><Users size={13} /> 새로운 기능</span>
          <h2 id="crew-feature-title">크루별<br /><em>모아보기가 추가됐어요.</em></h2>
          <p>크루별 모아보기에서 크루 이름을 누르면 각 크루가 만든 서비스를 한곳에서 확인할 수 있습니다. (서비스 대시보드 &gt; 헤더에 크루 클릭)</p>
        </div>
        <label className="feature-notice-modal__check"><input type="checkbox" checked={hideToday} onChange={(event) => setHideToday(event.target.checked)} /> <span>오늘 하루 동안 보지 않기</span></label>
        <div className="feature-notice-modal__actions">
          <Link to="/makers" className="button button--secondary" onClick={() => onClose(false)}>크루별 모아보기</Link>
          <button type="button" className="button button--primary" onClick={dismiss}>확인했어요</button>
        </div>
      </section>
    </div>
  )
}
