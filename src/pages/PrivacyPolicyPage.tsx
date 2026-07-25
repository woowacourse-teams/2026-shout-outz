import { ArrowLeft, ExternalLink, Settings2, ShieldCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { AnalyticsConsent } from '../utils/analytics'

interface PrivacyPolicyPageProps {
  consent: AnalyticsConsent | null
  onChangeAnalyticsSettings: () => void
  onSetAnalyticsConsent: (consent: AnalyticsConsent) => void
}

export function PrivacyPolicyPage({ consent, onChangeAnalyticsSettings, onSetAnalyticsConsent }: PrivacyPolicyPageProps) {
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
            <p>이 페이지는 현재 Dropit에서 사용하는 Google Analytics 4(GA4)의 수집·이용 내용을 안내합니다.</p>
          </div>
          <p className="privacy-document__updated">시행일: 2026년 7월 25일</p>

          <section>
            <h2>1. 수집 목적</h2>
            <p>Dropit은 방문 규모와 기능 이용 흐름을 파악하고 서비스의 안정성과 사용성을 개선하기 위해 GA4를 사용합니다.</p>
          </section>

          <section>
            <h2>2. 수집될 수 있는 정보</h2>
            <ul>
              <li>방문한 페이지와 서비스 이용 이벤트</li>
              <li>브라우저, 운영체제, 기기 정보 및 대략적인 이용 환경</li>
              <li>쿠키 또는 유사한 기술을 통한 식별자</li>
              <li>로그인한 사용자의 서비스 이용 흐름을 연결하기 위한 내부 식별자(User-ID)</li>
            </ul>
            <p>이메일 주소와 이름을 Google Analytics로 전송하지 않습니다. 다만 내부 식별자도 이용자를 구분하는 정보이므로 이 안내에 포함해 공개합니다.</p>
          </section>

          <section>
            <h2>3. 이용자의 선택</h2>
            <p>분석 동의 후에만 GA4를 초기화하고 측정 데이터를 전송합니다. 거부한 경우 GA4를 실행하지 않습니다. 하단의 분석 설정에서 언제든 선택을 다시 변경할 수 있습니다.</p>
            <div className="privacy-document__consent">
              <div>
                <strong>현재 분석 설정</strong>
                <span>{consent === 'granted' ? '허용됨' : consent === 'denied' ? '거부됨' : '선택하지 않음'}</span>
              </div>
              {consent === null ? (
                <div className="privacy-document__actions">
                  <button type="button" className="button button--secondary button--small" onClick={() => onSetAnalyticsConsent('denied')}>분석 거부</button>
                  <button type="button" className="button button--primary button--small" onClick={() => onSetAnalyticsConsent('granted')}>분석 허용</button>
                </div>
              ) : (
                <button type="button" className="button button--secondary button--small" onClick={onChangeAnalyticsSettings}><Settings2 size={14} /> 다시 선택하기</button>
              )}
            </div>
          </section>

          <section>
            <h2>4. 제3자 서비스</h2>
            <p>GA4는 Google LLC가 제공하는 분석 서비스입니다. Google의 데이터 처리 방식은 Google의 개인정보처리방침에서 확인할 수 있습니다.</p>
            <a className="privacy-document__external-link" href="https://policies.google.com/privacy?hl=ko" target="_blank" rel="noreferrer">Google 개인정보처리방침 <ExternalLink size={14} /></a>
          </section>

          <p className="privacy-document__note">서비스 운영 범위가 변경되면 수집 항목과 처리 목적을 이 페이지에 함께 업데이트하겠습니다.</p>
        </article>
      </div>
    </div>
  )
}
