import { Outlet } from 'react-router-dom'
import { Header } from './Header'
import { SiteFooter } from './SiteFooter'
import type { VisitorStats } from '../types'
import type { AuthUser } from '../utils/auth'

interface LayoutProps {
  authUser: AuthUser | null
  onLogout: () => void
  visitorStats: VisitorStats | null
}

export function Layout({ authUser, onLogout, visitorStats }: LayoutProps) {
  return (
    <div className="site-shell">
      <Header authUser={authUser} onLogout={onLogout} />
      <main>
        <Outlet />
      </main>
      <SiteFooter authUser={authUser} visitorStats={visitorStats} />
    </div>
  )
}
