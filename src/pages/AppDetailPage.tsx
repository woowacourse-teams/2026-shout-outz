import { useEffect, useState } from 'react'
import { ArrowLeft, ArrowUpRight, Bookmark, ExternalLink, Heart, Pencil, Play, Share2, Trash2 } from 'lucide-react'
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
  onDeleteApp: (id: string) => Promise<boolean>
}

export function AppDetailPage({ apps, profile, bookmarkedIds, likedIds, onToggleBookmark, onToggleLike, onLaunch, onDeleteApp }: AppDetailPageProps) {
  const { appId } = useParams()
  const navigate = useNavigate()
  const [isDeleting, setIsDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState('')
  const app = apps.find((item) => item.id === appId)

  useEffect(() => {
    if (!app) return

    const defaultTitle = 'Dropit - 우테코 크루 서비스 아카이빙'
    const defaultDescription = '작은 웹앱을 발견하고 바로 실행하는 공간, Dropit'
    const title = `${app.name} | Dropit`
    const description = app.tagline || defaultDescription
    const image = app.thumbnailUrl && /^https?:\/\//i.test(app.thumbnailUrl)
      ? app.thumbnailUrl
      : 'https://drop-it-project.vercel.app/dropit-og.png'

    document.title = title
    const metadata = [
      ['description', description],
      ['og:title', title],
      ['og:description', description],
      ['og:url', window.location.href],
      ['og:image', image],
      ['og:image:alt', `${app.name} 서비스 썸네일`],
      ['twitter:title', title],
      ['twitter:description', description],
      ['twitter:image', image],
      ['twitter:image:alt', `${app.name} 서비스 썸네일`],
    ]

    metadata.forEach(([key, content]) => {
      const selector = key === 'description' ? 'meta[name="description"]' : `meta[property="${key}"], meta[name="${key}"]`
      const element = document.head.querySelector<HTMLMetaElement>(selector)
      if (element) element.content = content
    })

    return () => {
      document.title = defaultTitle
      const defaults: Record<string, string> = {
        description: defaultDescription,
        'og:title': defaultTitle,
        'og:description': defaultDescription,
        'og:url': 'https://drop-it-project.vercel.app/',
        'og:image': 'https://drop-it-project.vercel.app/dropit-og.png',
        'og:image:alt': 'Dropit 서비스 화면',
        'twitter:title': defaultTitle,
        'twitter:description': defaultDescription,
        'twitter:image': 'https://drop-it-project.vercel.app/dropit-og.png',
        'twitter:image:alt': 'Dropit 서비스 화면',
      }
      Object.entries(defaults).forEach(([key, content]) => {
        const selector = key === 'description' ? 'meta[name="description"]' : `meta[property="${key}"], meta[name="${key}"]`
        const element = document.head.querySelector<HTMLMetaElement>(selector)
        if (element) element.content = content
      })
    }
  }, [app])

  if (!app) {
    return <div className="page-pad"><EmptyState type="app" onReset={() => navigate('/')} /></div>
  }

  const similarApps = apps.filter((item) => item.id !== app.id && item.categories.some((category) => app.categories.includes(category))).slice(0, 3)
  const isBookmarked = bookmarkedIds.includes(app.id)
  const isLiked = likedIds.includes(app.id)
  const isOwner = app.source === 'submitted' && profile != null && (app.ownerId ?? app.maker.id) === profile.id

  const launch = () => {
    onLaunch(app.id)
    if (!app.appUrl) return
    const popup = window.open(app.appUrl, '_blank', 'noopener,noreferrer')
    if (popup) popup.opener = null
  }

  const removeApp = async () => {
    if (isDeleting || !window.confirm('이 앱을 삭제할까요?\n삭제하면 되돌릴 수 없습니다.')) return
    setIsDeleting(true)
    setDeleteError('')
    try {
      const didDelete = await onDeleteApp(app.id)
      if (!didDelete) setDeleteError('앱을 삭제하지 못했습니다. 잠시 후 다시 시도해주세요.')
    } catch {
      setDeleteError('앱을 삭제하지 못했습니다. 잠시 후 다시 시도해주세요.')
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <div className="detail-page">
      <section className="detail-hero">
        <div className="content-container">
          <Link to="/" className="back-link"><ArrowLeft size={15} /> 전체 서비스</Link>
          <div className="detail-hero__grid">
            <div className="detail-hero__visual"><AppThumbnail variant={app.thumbnailVariant} label={app.name} size="large" customImage={app.thumbnailUrl} /></div>
            <div className="detail-hero__copy">
              <div className="detail-hero__eyebrow"><span>{app.categories.join(' · ')}</span><span>앱 #{String(apps.findIndex((item) => item.id === app.id) + 1).padStart(2, '0')}</span></div>
              <h1>{app.name}</h1>
              <p className="detail-hero__tagline">{app.tagline}</p>
              {isOwner ? <div className="detail-owner-actions">
                <Link to={`/apps/${app.id}/edit`} className="button button--secondary button--small"><Pencil size={14} /> 내 앱 수정</Link>
                <button type="button" className="button button--danger button--small" onClick={removeApp} disabled={isDeleting}><Trash2 size={14} /> {isDeleting ? '삭제 중...' : '앱 삭제하기'}</button>
              </div> : null}
              {isOwner && deleteError ? <p className="detail-delete-error" role="alert">{deleteError}</p> : null}
              <div className="detail-actions">
                <button type="button" className="button button--primary button--large" onClick={launch}>앱 실행하기 <ArrowUpRight size={17} /></button>
                <div className="detail-actions__secondary">
                  <button type="button" className={`reaction-button ${isLiked ? 'is-active' : ''}`} onClick={() => onToggleLike(app.id)} aria-pressed={isLiked}><Heart size={17} fill={isLiked ? 'currentColor' : 'none'} /><span>{formatNumber(app.likes)}</span></button>
                  <button type="button" className={`reaction-button ${isBookmarked ? 'is-active' : ''}`} onClick={() => onToggleBookmark(app.id)} aria-pressed={isBookmarked}><Bookmark size={17} fill={isBookmarked ? 'currentColor' : 'none'} /><span>{isBookmarked ? '저장됨' : '저장'}</span></button>
                  <button type="button" className="reaction-button" onClick={() => {
                    const shareData = { title: `${app.name} | Dropit`, text: app.tagline, url: window.location.href }
                    if (navigator.share) {
                      void navigator.share(shareData).catch(() => undefined)
                      return
                    }
                    void navigator.clipboard?.writeText(window.location.href)
                  }}><Share2 size={16} /><span>공유</span></button>
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
