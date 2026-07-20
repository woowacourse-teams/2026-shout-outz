import { Outlet } from 'react-router-dom'
import { Header } from './Header'
import { SiteFooter } from './SiteFooter'
import type { AuthUser } from '../utils/auth'

interface LayoutProps {
  authUser: AuthUser | null
  onLogout: () => void
}

export function Layout({ authUser, onLogout }: LayoutProps) {
  return (
    <div className="site-shell">
      <Header authUser={authUser} onLogout={onLogout} />
      <main>
        <Outlet />
      </main>
      <SiteFooter />
    </div>
  )
}
