import { Bookmark, LogIn, LogOut, Menu, Search, X } from 'lucide-react'
import { FormEvent, useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import type { AuthUser } from '../utils/auth'

interface HeaderProps {
  authUser: AuthUser | null
  onLogout: () => void
}

export function Header({ authUser, onLogout }: HeaderProps) {
  const navigate = useNavigate()
  const location = useLocation()
  const [query, setQuery] = useState('')
  const [menuOpen, setMenuOpen] = useState(false)

  useEffect(() => {
    const params = new URLSearchParams(location.search)
    setQuery(params.get('q') ?? '')
    setMenuOpen(false)
  }, [location.pathname, location.search])

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const trimmed = query.trim()
    navigate(trimmed ? `/?q=${encodeURIComponent(trimmed)}` : '/')
  }

  return (
    <>
      <header className="global-nav">
        <div className="global-nav__inner">
          <Link to="/" className="brand" aria-label="Dropit 홈"><img src="/dropit-icon.svg" alt="" className="brand__icon" /><span className="brand__word">dropit<span>.</span></span></Link>
          <nav className={`global-nav__links ${menuOpen ? 'is-open' : ''}`} aria-label="주요 메뉴">
            <NavLink to="/" end>탐색</NavLink>
            <NavLink to="/bookmarks"><Bookmark size={14} /> 저장한 앱</NavLink>
            {authUser ? <NavLink to="/makers/me">내 프로필</NavLink> : null}
            <NavLink to="/submit">앱 등록하기</NavLink>
          </nav>
          <div className="global-nav__actions">
            <form className="nav-search" onSubmit={handleSearch} role="search">
              <Search size={14} aria-hidden="true" />
              <label htmlFor="nav-search-input" className="sr-only">앱 검색</label>
              <input
                id="nav-search-input"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="앱 검색"
                type="search"
              />
            </form>
            {authUser ? <div className="auth-actions"><Link to="/makers/me" className="auth-user" title="내 프로필"><span>{authUser.name}</span></Link><button type="button" className="auth-logout" onClick={onLogout}><LogOut size={14} /> 로그아웃</button></div> : <Link to="/login" className="header-login"><LogIn size={14} /> 로그인</Link>}
            <button type="button" className="mobile-menu-button" aria-label={menuOpen ? '메뉴 닫기' : '메뉴 열기'} aria-expanded={menuOpen} onClick={() => setMenuOpen((open) => !open)}>
              {menuOpen ? <X size={18} /> : <Menu size={18} />}
            </button>
          </div>
        </div>
      </header>
      <div className="sub-nav">
        <div className="sub-nav__inner">
          <Link to="/" className="sub-nav__title">Dropit</Link>
          <span className="sub-nav__divider" aria-hidden="true" />
          <span className="sub-nav__context">서비스 공유하기</span>
          <Link to="/submit" className="button button--primary button--small sub-nav__cta">앱 등록하기</Link>
        </div>
      </div>
    </>
  )
}
