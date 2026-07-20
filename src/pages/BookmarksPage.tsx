import { Bookmark } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { AppItem } from '../types'
import { AppCard } from '../components/AppCard'
import { EmptyState } from '../components/EmptyState'

export function BookmarksPage({ apps, bookmarkedIds, onToggleBookmark }: { apps: AppItem[]; bookmarkedIds: string[]; onToggleBookmark: (id: string) => void }) {
  const bookmarkedApps = apps.filter((app) => bookmarkedIds.includes(app.id))
  return (
    <div className="bookmarks-page page-pad">
      <div className="content-container">
        <div className="page-heading"><span className="section-kicker"><Bookmark size={14} /> 저장한 앱</span><h1>저장한 앱</h1><p>다시 보고 싶은 앱을 저장해두세요.</p></div>
        {bookmarkedApps.length > 0 ? <div className="app-grid">{bookmarkedApps.map((app) => <AppCard key={app.id} app={app} isBookmarked={true} onToggleBookmark={onToggleBookmark} />)}</div> : <EmptyState />}
        <div className="page-heading__bottom"><Link to="/" className="text-link">앱 더 둘러보기</Link></div>
      </div>
    </div>
  )
}
