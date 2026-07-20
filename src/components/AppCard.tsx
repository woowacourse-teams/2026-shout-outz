import { Bookmark, ExternalLink, Heart, Play } from 'lucide-react'
import type { KeyboardEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import type { AppItem } from '../types'
import { formatNumber } from '../utils/format'
import { AppThumbnail } from './AppThumbnail'
import { Avatar } from './Avatar'
import { IconButton } from './IconButton'

interface AppCardProps {
  app: AppItem
  isBookmarked: boolean
  onToggleBookmark: (id: string) => void
  featured?: boolean
}

export function AppCard({ app, isBookmarked, onToggleBookmark, featured = false }: AppCardProps) {
  const navigate = useNavigate()

  const goToApp = () => navigate(`/apps/${app.id}`)

  const handleKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    if (event.target !== event.currentTarget) return
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      goToApp()
    }
  }

  return (
    <article
      className={`app-card ${featured ? 'app-card--featured' : ''}`}
      role="link"
      tabIndex={0}
      onClick={goToApp}
      onKeyDown={handleKeyDown}
    >
      <div className="app-card__visual">
        <AppThumbnail variant={app.thumbnailVariant} label={app.name} size={featured ? 'large' : 'medium'} customImage={app.thumbnailUrl} />
        <div className="app-card__visual-tools">
          <span className="app-card__open-hint"><ExternalLink size={13} /> 자세히 보기</span>
          <IconButton
            label={isBookmarked ? `${app.name} 북마크 해제` : `${app.name} 북마크 저장`}
            active={isBookmarked}
            className="icon-button--on-image"
            onClick={(event) => {
              event.stopPropagation()
              onToggleBookmark(app.id)
            }}
          >
            <Bookmark size={17} fill={isBookmarked ? 'currentColor' : 'none'} />
          </IconButton>
        </div>
      </div>
      <div className="app-card__body">
        <div className="app-card__eyebrow">
          <span>{app.category}</span>
          <span className="app-card__date">{new Date(app.createdAt).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })}</span>
        </div>
        <h3>{app.name}</h3>
        <p>{app.tagline}</p>
        <div className="app-card__footer">
          <button
            type="button"
            className="maker-chip"
            onClick={(event) => {
              event.stopPropagation()
              navigate(`/makers/${app.maker.id}`)
            }}
          >
            <Avatar maker={app.maker} />
            <span>{app.maker.name}</span>
          </button>
          <span className="app-card__stats">
            <span><Play size={13} fill="currentColor" /> {formatNumber(app.plays)}</span>
            <span><Heart size={13} fill="currentColor" /> {formatNumber(app.likes)}</span>
          </span>
        </div>
      </div>
    </article>
  )
}
