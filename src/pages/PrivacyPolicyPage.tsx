import { ArrowLeft, ExternalLink, Settings2, ShieldCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { getConfiguredAnalyticsServices, type AnalyticsConsent } from '../utils/analytics'

interface PrivacyPolicyPageProps {
  consent: AnalyticsConsent | null
  onChangeAnalyticsSettings: () => void
  onSetAnalyticsConsent: (consent: AnalyticsConsent) => void
}

export function PrivacyPolicyPage({ consent, onChangeAnalyticsSettings, onSetAnalyticsConsent }: PrivacyPolicyPageProps) {
  const services = getConfiguredAnalyticsServices()
  const serviceNames = services.map(({ name }) => name).join('와 ')
  const noConsentServices = services.filter((service) => !service.requiresConsent)
  const noConsentServiceNames = noConsentServices.map(({ name }) => name).join('와 ')
  const consentServices = services.filter((service) => service.requiresConsent)
  const consentServiceNames = consentServices.map(({ name }) => name).join('와 ')
  const crossBorderServices = services.filter((service) => service.crossBorderTransfer)

  return (
    <div className="privacy-page page-pad">
      <div className="content-container privacy-page__container">
        <Link to="/" className="back-link"><ArrowLeft size={16} /> 홈으로 돌아가기</Link>
        <header className="privacy-page__heading page-heading">
          <span className="section-kicker"><i /> Dropit 개인정보 안내</span>
          <h1>개인정보 처리방침</h1>
          <p>Dropit은 서비스 이용에 필요한 정보를 투명하게 안내하고, 서비스 개선에 필요한 범위에서만 데이터를 사용합니다.</p>
        </header>

        <article className="privacy-document">
          <div className="privacy-document__intro">
            <ShieldCheck size={20} />
            <p>이 페이지는 현재 Dropit에서 사용하는 {serviceNames}의 수집·이용 내용을 안내합니다.</p>
          </div>
          <p className="privacy-document__updated">시행일: 2026년 8월 14일</p>

          <section>
            <h2>1. 수집 목적</h2>
            {services.map((service) => <p key={service.id}>Dropit은 {service.purpose}하기 위해 {service.name}를 사용합니다.</p>)}
          </section>

          <section>
            <h2>2. 수집될 수 있는 정보</h2>
            <ul>
              <li>브라우저, 운영체제, 기기 정보 및 대략적인 이용 환경</li>
              <li>쿠키 또는 유사한 기술을 통한 식별자</li>
              {[...new Set(services.flatMap(({ collectedData }) => collectedData))].map((item) => <li key={item}>{item}</li>)}
            </ul>
            <p>이메일 주소, 이름, 회원 식별자를 포함한 직접 식별정보는 {serviceNames}로 전송하지 않습니다.</p>
          </section>

          <section>
            <h2>3. 이용자의 선택</h2>
            {noConsentServices.length > 0 ? <p>{noConsentServiceNames}는 방문 통계 측정을 위한 서비스로, 국내 개인정보 보호 법령상 별도 동의 없이 수집됩니다.</p> : null}
            {consentServices.length > 0 ? (
              <>
                <p>{consentServiceNames}는 화면 조작을 기록하는 방식으로 동작하여, 동의하신 경우에만 초기화되고 데이터를 수집합니다. 거부하신 경우 실행하지 않습니다. 하단에서 언제든 선택을 다시 변경할 수 있습니다.</p>
                <div className="privacy-document__consent">
                  <div>
                    <strong>현재 {consentServiceNames} 수집 동의 설정</strong>
                    <span>{consent === 'granted' ? '허용됨' : consent === 'denied' ? '거부됨' : '선택하지 않음'}</span>
                  </div>
                  {consent === null ? (
                    <div className="privacy-document__actions">
                      <button type="button" className="button button--secondary button--small" onClick={() => onSetAnalyticsConsent('denied')}>수집 거부</button>
                      <button type="button" className="button button--primary button--small" onClick={() => onSetAnalyticsConsent('granted')}>수집 허용</button>
                    </div>
                  ) : (
                    <button type="button" className="button button--secondary button--small" onClick={onChangeAnalyticsSettings}><Settings2 size={14} /> 다시 선택하기</button>
                  )}
                </div>
              </>
            ) : null}
          </section>

          <section>
            <h2>4. 제3자 서비스</h2>
            {services.map((service) => (
              <div key={service.id}>
                <p>{service.name}는 {service.provider}가 제공하는 분석 서비스입니다. 데이터 처리 방식은 제공자의 개인정보처리방침에서 확인할 수 있습니다.</p>
                <a className="privacy-document__external-link" href={service.privacyPolicyUrl} target="_blank" rel="noreferrer">{service.provider} 개인정보처리방침 <ExternalLink size={14} /></a>
              </div>
            ))}
          </section>

          {crossBorderServices.length > 0 ? (
            <section>
              <h2>5. 개인정보의 국외 이전</h2>
              <p>Dropit은 다음과 같이 개인정보를 국외로 이전하며, 이용자의 동의를 받은 경우에만 이전합니다.</p>
              {crossBorderServices.map((service) => (
                <div key={service.id} className="privacy-document__transfer">
                  <p><strong>{service.name}</strong></p>
                  <ul>
                    <li>이전받는 자: {service.crossBorderTransfer!.recipient}</li>
                    <li>이전되는 국가: {service.crossBorderTransfer!.country}</li>
                    <li>이전 일시 및 방법: {service.crossBorderTransfer!.transferMethod}</li>
                    <li>이전되는 항목: {service.crossBorderTransfer!.items.join(', ')}</li>
                    <li>이용 목적 및 보유·이용 기간: {service.crossBorderTransfer!.purposeAndRetention}</li>
                  </ul>
                </div>
              ))}
            </section>
          ) : null}

          <p className="privacy-document__note">서비스 운영 범위가 변경되면 수집 항목과 처리 목적을 이 페이지에 함께 업데이트하겠습니다.</p>
        </article>
      </div>
    </div>
  )
}
