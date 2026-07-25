import { ArrowLeft, ArrowUpRight, Bookmark, Heart, Play, Users } from 'lucide-react'
import type { CSSProperties } from 'react'
import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { AppThumbnail } from '../components/AppThumbnail'
import { Avatar } from '../components/Avatar'
import { EmptyState } from '../components/EmptyState'
import { IconButton } from '../components/IconButton'
import type { AppItem, Maker } from '../types'
import { formatNumber } from '../utils/format'

interface MakersPageProps {
  apps: AppItem[]
  bookmarkedIds: string[]
  onToggleBookmark: (id: string) => void
}

interface MakerCollection {
  maker: Maker
  apps: AppItem[]
  plays: number
  likes: number
}

export function MakersPage({ apps, bookmarkedIds, onToggleBookmark }: MakersPageProps) {
  const collections = useMemo(() => {
    const grouped = new Map<string, MakerCollection>()

    apps.forEach((app) => {
      const current = grouped.get(app.maker.id)
      if (current) {
        current.apps.push(app)
        current.plays += app.plays
        current.likes += app.likes
        return
      }

      grouped.set(app.maker.id, {
        maker: app.maker,
        apps: [app],
        plays: app.plays,
        likes: app.likes,
      })
    })

    return [...grouped.values()]
      .map((collection) => ({
        ...collection,
        apps: [...collection.apps].sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
      }))
      .sort((a, b) => b.apps.length - a.apps.length || b.plays - a.plays || a.maker.name.localeCompare(b.maker.name, 'ko'))
  }, [apps])

  const totalPlays = useMemo(() => apps.reduce((total, app) => total + app.plays, 0), [apps])

  return (
    <div className="makers-page">
      <section className="makers-hero">
        <div className="content-container">
          <Link to="/" className="back-link"><ArrowLeft size={15} /> 전체 서비스</Link>
          <div className="makers-hero__layout">
            <div className="makers-hero__copy">
              <span className="section-kicker section-kicker--on-dark"><Users size={13} /> 크루 디렉터리</span>
              <h1>크루별<br /><em>모아보기</em></h1>
              <p>Dropit에 서비스를 남긴 크루들의 작업을 한곳에서 둘러보고, 각자의 프로필에서 더 많은 서비스를 확인해보세요.</p>
            </div>
            <div className="makers-hero__stats" aria-label="크루 및 서비스 통계">
              <div><strong>{formatNumber(collections.length)}</strong><span>서비스를 만든 크루</span></div>
              <div><strong>{formatNumber(apps.length)}</strong><span>등록된 서비스</span></div>
              <div><strong>{formatNumber(totalPlays)}</strong><span>누적 실행</span></div>
            </div>
          </div>
        </div>
      </section>

      <section className="makers-directory">
        <div className="content-container">
          {collections.length > 0 ? <nav className="makers-crew-nav" aria-label="크루 바로가기">
            {collections.map((collection) => <a className="makers-crew-nav__item" href={`#maker-${collection.maker.id}`} key={collection.maker.id}>
              <Avatar maker={collection.maker} size="medium" />
              <span>{collection.maker.name}</span>
            </a>)}
          </nav> : null}
          <div className="section-heading">
            <div>
              <span className="section-kicker">크루별 모아보기</span>
              <h2>각자의 실험실</h2>
            </div>
            <span className="section-heading__note">{formatNumber(apps.length)}개 서비스</span>
          </div>

          {collections.length > 0 ? <div className="maker-collections">
            {collections.map((collection, index) => (
              <article className="maker-collection" id={`maker-${collection.maker.id}`} key={collection.maker.id} style={{ '--maker-tone': collection.maker.tone } as CSSProperties}>
                <div className="maker-collection__identity">
                  <Link to={`/makers/${collection.maker.id}`} className="maker-collection__profile">
                    <Avatar maker={collection.maker} size="medium" />
                    <span>
                      <small>크루 {String(index + 1).padStart(2, '0')}</small>
                      <strong>{collection.maker.name}</strong>
                      <em>{collection.maker.role}</em>
                    </span>
                  </Link>
                  <p className="maker-collection__bio">{collection.maker.bio}</p>
                  <div className="maker-collection__stats">
                    <span><strong>{collection.apps.length}</strong><small>서비스</small></span>
                    <span><strong>{formatNumber(collection.plays)}</strong><small>실행</small></span>
                    <span><strong>{formatNumber(collection.likes)}</strong><small>좋아요</small></span>
                  </div>
                  <Link to={`/makers/${collection.maker.id}`} className="text-link maker-collection__profile-link">프로필 보기 <ArrowUpRight size={14} /></Link>
                </div>

                <div className="maker-collection__services">
                  <div className="maker-collection__services-heading"><span>등록한 서비스</span><small>최신순</small></div>
                  <div className="maker-service-list">
                    {collection.apps.map((app) => {
                      const isBookmarked = bookmarkedIds.includes(app.id)
                      return (
                        <article className="maker-service-card" key={app.id}>
                          <Link to={`/apps/${app.id}`} className="maker-service-card__link">
                            <div className="maker-service-card__thumbnail"><AppThumbnail variant={app.thumbnailVariant} label={app.name} size="medium" customImage={app.thumbnailUrl} /></div>
                            <div className="maker-service-card__body">
                              <span className="maker-service-card__categories">{app.categories.join(' · ')}</span>
                              <h3>{app.name}</h3>
                              <p>{app.tagline}</p>
                              <span className="maker-service-card__stats"><span><Play size={12} fill="currentColor" /> {formatNumber(app.plays)}</span><span><Heart size={12} fill="currentColor" /> {formatNumber(app.likes)}</span></span>
                            </div>
                          </Link>
                          <IconButton
                            label={isBookmarked ? `${app.name} 북마크 해제` : `${app.name} 북마크 저장`}
                            active={isBookmarked}
                            className="maker-service-card__bookmark"
                            onClick={() => onToggleBookmark(app.id)}
                          >
                            <Bookmark size={15} fill={isBookmarked ? 'currentColor' : 'none'} />
                          </IconButton>
                        </article>
                      )
                    })}
                  </div>
                </div>
              </article>
            ))}
          </div> : <EmptyState type="catalog" />}
        </div>
      </section>
    </div>
  )
}
