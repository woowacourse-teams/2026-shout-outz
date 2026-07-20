import { ArrowLeft, Code2, LogIn } from 'lucide-react'
import { Link } from 'react-router-dom'

interface LoginPageProps {
  isConfigured: boolean
  isLoading: boolean
  error: string
  notice?: string
  returnTo?: string
  onLogin: (redirectTo: string) => void
}

export function LoginPage({ isConfigured, isLoading, error, notice, returnTo = '/submit', onLogin }: LoginPageProps) {
  return (
    <div className="login-page page-pad">
      <div className="content-container">
        <Link to="/" className="back-link"><ArrowLeft size={15} /> 홈으로</Link>
        <section className="login-card" aria-labelledby="login-title">
          <div className="login-card__icon"><Code2 size={25} /></div>
          <span className="section-kicker">로그인</span>
          <h1 id="login-title">앱을 등록하려면<br /><em>로그인해주세요.</em></h1>
          <p>GitHub 계정으로 로그인하면 앱을 등록하고 내 앱을 관리할 수 있습니다.</p>
          {notice ? <p className="login-card__notice" role="status">{notice}</p> : null}
          <small className="login-card__scope-note">로그인에 필요한 기본 정보만 사용하며, 저장소 권한은 요청하지 않습니다.</small>
          <button type="button" className="button button--primary button--large login-card__button" onClick={() => onLogin(returnTo)} disabled={!isConfigured || isLoading}>
            <Code2 size={17} /> {isLoading ? '로그인 확인 중' : 'GitHub로 로그인'} <LogIn size={16} />
          </button>
          {!isConfigured ? <p className="login-card__note">Supabase 로그인 설정이 아직 연결되지 않았습니다. 환경변수를 설정한 뒤 다시 시작해주세요.</p> : null}
          {error ? <p className="login-card__error" role="alert">{error}</p> : null}
        </section>
      </div>
    </div>
  )
}
