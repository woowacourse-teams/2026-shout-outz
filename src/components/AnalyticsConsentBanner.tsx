import { ShieldCheck } from 'lucide-react'
import { Link } from 'react-router-dom'

interface AnalyticsConsentBannerProps {
  onAccept: () => void
  onReject: () => void
}

export function AnalyticsConsentBanner({ onAccept, onReject }: AnalyticsConsentBannerProps) {
  return (
    <aside className="analytics-consent-banner" role="dialog" aria-labelledby="analytics-consent-title" aria-describedby="analytics-consent-description">
      <div className="analytics-consent-banner__copy">
        <span className="analytics-consent-banner__eyebrow"><ShieldCheck size={15} /> 개인정보 안내</span>
        <h2 id="analytics-consent-title">서비스 이용 통계를 확인해도 될까요?</h2>
        <p id="analytics-consent-description">Dropit은 서비스 개선을 위해 Google Analytics 4를 사용합니다. 방문 페이지와 기능 이용 정보, 브라우저·기기 정보가 수집될 수 있습니다. <Link to="/privacy">자세히 보기</Link></p>
      </div>
      <div className="analytics-consent-banner__actions">
        <button type="button" className="analytics-consent-banner__reject" onClick={onReject}>거부</button>
        <button type="button" className="analytics-consent-banner__accept" onClick={onAccept}>동의하고 계속하기</button>
      </div>
    </aside>
  )
}
