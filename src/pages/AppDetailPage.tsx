import { ArrowLeft, ArrowUpRight, Bookmark, ExternalLink, Heart, Pencil, Play, Share2 } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import type { AppItem, Maker } from '../types'
import { AppCard } from '../components/AppCard'
import { AppThumbnail } from '../components/AppThumbnail'
import { Avatar } from '../components/Avatar'
import { EmptyState } from '../components/EmptyState'
import { MarkdownContent } from '../components/MarkdownContent'
import { formatNumber } from '../utils/format'

interface AppDetailPageProps {
  apps: AppItem[]
  profile: Maker | null
  bookmarkedIds: string[]
  likedIds: string[]
  onToggleBookmark: (id: string) => void
  onToggleLike: (id: string) => void
  onLaunch: (id: string) => void
}

export function AppDetailPage({ apps, profile, bookmarkedIds, likedIds, onToggleBookmark, onToggleLike, onLaunch }: AppDetailPageProps) {
  const { appId } = useParams()
  const navigate = useNavigate()
  const app = apps.find((item) => item.id === appId)

  if (!app) {
    return <div className="page-pad"><EmptyState type="app" onReset={() => navigate('/')} /></div>
  }

  const similarApps = apps.filter((item) => item.category === app.category && item.id !== app.id).slice(0, 3)
  const isBookmarked = bookmarkedIds.includes(app.id)
  const isLiked = likedIds.includes(app.id)
  const isOwner = app.source === 'submitted' && profile != null && (app.ownerId ?? app.maker.id) === profile.id

  const launch = () => {
    onLaunch(app.id)
    if (!app.appUrl) return
    const popup = window.open(app.appUrl, '_blank', 'noopener,noreferrer')
    if (popup) popup.opener = null
  }

  return (
    <div className="detail-page">
      <section className="detail-hero">
        <div className="content-container">
          <Link to="/" className="back-link"><ArrowLeft size={15} /> 전체 서비스</Link>
          <div className="detail-hero__grid">
            <div className="detail-hero__visual"><AppThumbnail variant={app.thumbnailVariant} label={app.name} size="large" customImage={app.thumbnailUrl} /></div>
            <div className="detail-hero__copy">
              <div className="detail-hero__eyebrow"><span>{app.category}</span><span>앱 #{String(apps.findIndex((item) => item.id === app.id) + 1).padStart(2, '0')}</span></div>
              <h1>{app.name}</h1>
              <p className="detail-hero__tagline">{app.tagline}</p>
              {isOwner ? <Link to={`/apps/${app.id}/edit`} className="button button--secondary button--small detail-edit-link"><Pencil size={14} /> 내 앱 수정</Link> : null}
              <div className="detail-actions">
                <button type="button" className="button button--primary button--large" onClick={launch}>앱 실행하기 <ArrowUpRight size={17} /></button>
                <div className="detail-actions__secondary">
                  <button type="button" className={`reaction-button ${isLiked ? 'is-active' : ''}`} onClick={() => onToggleLike(app.id)} aria-pressed={isLiked}><Heart size={17} fill={isLiked ? 'currentColor' : 'none'} /><span>{formatNumber(app.likes)}</span></button>
                  <button type="button" className={`reaction-button ${isBookmarked ? 'is-active' : ''}`} onClick={() => onToggleBookmark(app.id)} aria-pressed={isBookmarked}><Bookmark size={17} fill={isBookmarked ? 'currentColor' : 'none'} /><span>{isBookmarked ? '저장됨' : '저장'}</span></button>
                  <button type="button" className="reaction-button" onClick={() => navigator.clipboard?.writeText(window.location.href)}><Share2 size={16} /><span>공유</span></button>
                </div>
              </div>
              <div className="detail-hero__meta">
                <span><Play size={14} fill="currentColor" /> {formatNumber(app.plays)}회 실행</span>
                <span>{new Date(app.createdAt).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })} 등록</span>
              </div>
            </div>
          </div>
        </div>
      </section>
      <section className="detail-info">
        <div className="content-container detail-info__grid">
          <div className="detail-info__main">
            <span className="section-kicker">서비스 소개</span>
            <h2>서비스 설명</h2>
            <MarkdownContent content={app.description} />
            <div className="tech-tags">{app.techTags.map((tag) => <span key={tag}>{tag}</span>)}</div>
          </div>
          <aside className="maker-panel">
            <span className="section-kicker">제작자</span>
            <Link to={`/makers/${app.maker.id}`} className="maker-panel__profile"><Avatar maker={app.maker} size="large" /><span><strong>{app.maker.name}</strong><small>{app.maker.role}</small></span><ArrowUpRight size={16} /></Link>
            <p>{app.maker.bio}</p>
            <div className="maker-panel__links">
              {app.githubUrl ? <a href={app.githubUrl} target="_blank" rel="noreferrer"><ArrowUpRight size={15} /> GitHub 바로가기 <ExternalLink size={12} /></a> : null}
              <a href={app.appUrl} target="_blank" rel="noreferrer"><ExternalLink size={15} /> 배포 링크 <ExternalLink size={12} /></a>
            </div>
          </aside>
        </div>
      </section>
      <section className="detail-similar">
        <div className="content-container">
          <div className="section-heading"><div><span className="section-kicker">더 둘러보기</span><h2>비슷한 앱</h2></div><Link to="/" className="text-link">전체 앱 보기 <ArrowUpRight size={15} /></Link></div>
          <div className="app-grid app-grid--three">{similarApps.map((item) => <AppCard key={item.id} app={item} isBookmarked={bookmarkedIds.includes(item.id)} onToggleBookmark={onToggleBookmark} />)}</div>
        </div>
      </section>
    </div>
  )
}
