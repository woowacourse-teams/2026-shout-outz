import { ArrowLeft, LogIn } from 'lucide-react'
import { Link } from 'react-router-dom'

function GitHubIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.084-.73.084-.73 1.205.084 1.84 1.237 1.84 1.237 1.07 1.835 2.807 1.305 3.492.998.108-.776.418-1.305.762-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.292-1.552 3.295-1.23 3.295-1.23.647 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.21 0 1.595-.015 2.88-.015 3.27 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12" />
    </svg>
  )
}

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
          <h1 id="login-title">앱을 등록하려면<br /><em>로그인해주세요.</em></h1>
          <p>로그인하면 앱을 등록하고 관리할 수 있습니다.</p>
          {notice ? <p className="login-card__notice" role="status">{notice}</p> : null}
          <small className="login-card__scope-note">로그인에 필요한 기본 정보만 사용하며, 저장소 권한은 요청하지 않습니다.</small>
          <button type="button" className="button button--primary button--large login-card__button" onClick={() => onLogin(returnTo)} disabled={!isConfigured || isLoading}>
            <GitHubIcon /> {isLoading ? '로그인 확인 중' : 'GitHub로 로그인'} <LogIn size={16} />
          </button>
          {!isConfigured ? <p className="login-card__note">Supabase 로그인 설정이 아직 연결되지 않았습니다. 환경변수를 설정한 뒤 다시 시작해주세요.</p> : null}
          {error ? <p className="login-card__error" role="alert">{error}</p> : null}
        </section>
      </div>
    </div>
  )
}
