import { ShieldCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { getConfiguredAnalyticsServices } from '../utils/analytics'

interface AnalyticsConsentBannerProps {
  onAccept: () => void
  onReject: () => void
}

export function AnalyticsConsentBanner({ onAccept, onReject }: AnalyticsConsentBannerProps) {
  const consentServices = getConfiguredAnalyticsServices().filter((service) => service.requiresConsent)
  const serviceNames = consentServices.map(({ name }) => name).join('와 ')
  const transfersOverseas = consentServices.some((service) => service.crossBorderTransfer)

  return (
    <aside className="analytics-consent-banner" role="dialog" aria-labelledby="analytics-consent-title" aria-describedby="analytics-consent-description">
      <div className="analytics-consent-banner__copy">
        <span className="analytics-consent-banner__eyebrow"><ShieldCheck size={15} /> 개인정보 안내</span>
        <h2 id="analytics-consent-title">Dropit 사용 경험 개선에 도움을 주시겠어요?</h2>
        <p id="analytics-consent-description">Dropit은 UX 개선을 위해 {serviceNames}로 화면 조작(세션 리플레이·히트맵) 정보를 수집합니다.{transfersOverseas ? ' 수집된 정보는 국외로 이전되어 처리될 수 있습니다.' : ''} 방문 통계(Google Analytics)는 이 동의와 무관하게 항상 수집됩니다. <Link to="/privacy">자세히 보기</Link></p>
      </div>
      <div className="analytics-consent-banner__actions">
        <button type="button" className="analytics-consent-banner__reject" onClick={onReject}>거부</button>
        <button type="button" className="analytics-consent-banner__accept" onClick={onAccept}>동의하고 계속하기</button>
      </div>
    </aside>
  )
}
