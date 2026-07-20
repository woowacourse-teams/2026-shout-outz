import { Bookmark, LogIn, LogOut, Menu, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import type { AuthUser } from '../utils/auth'

interface HeaderProps {
  authUser: AuthUser | null
  onLogout: () => void
}

export function Header({ authUser, onLogout }: HeaderProps) {
  const location = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)

  useEffect(() => {
    setMenuOpen(false)
  }, [location.pathname, location.search])

  return (
    <>
      <header className="global-nav">
        <div className="global-nav__inner">
          <Link to="/" className="brand" aria-label="Dropit 홈"><img src="/dropit-icon.svg" alt="" className="brand__icon" /><span className="brand__word">dropit<span>.</span></span></Link>
          <nav className={`global-nav__links ${menuOpen ? 'is-open' : ''}`} aria-label="주요 메뉴">
            <NavLink to="/" end>탐색</NavLink>
            {authUser ? <NavLink to="/bookmarks"><Bookmark size={14} /> 저장한 앱</NavLink> : null}
            {authUser ? <NavLink to="/makers/me">내 프로필</NavLink> : null}
            <NavLink to="/submit">앱 등록하기</NavLink>
          </nav>
          <div className="global-nav__actions">
            {authUser ? <div className="auth-actions"><Link to="/makers/me" className="auth-user" title="내 프로필"><span>{authUser.name}</span></Link><button type="button" className="auth-logout" onClick={onLogout}><LogOut size={14} /> 로그아웃</button></div> : <Link to="/login" className="header-login"><LogIn size={14} /> 로그인</Link>}
            <button type="button" className="mobile-menu-button" aria-label={menuOpen ? '메뉴 닫기' : '메뉴 열기'} aria-expanded={menuOpen} onClick={() => setMenuOpen((open) => !open)}>
              {menuOpen ? <X size={18} /> : <Menu size={18} />}
            </button>
          </div>
        </div>
      </header>
    </>
  )
}
