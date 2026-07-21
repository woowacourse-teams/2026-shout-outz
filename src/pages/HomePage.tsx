import { ArrowRight } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import type { AppItem, Category } from '../types'
import { CATEGORIES } from '../types'
import { AppCard } from '../components/AppCard'
import { DropZone } from '../components/DropZone'
import { EmptyState } from '../components/EmptyState'
import { formatNumber } from '../utils/format'

interface HomePageProps {
  apps: AppItem[]
  bookmarkedIds: string[]
  onToggleBookmark: (id: string) => void
}

export function HomePage({ apps, bookmarkedIds, onToggleBookmark }: HomePageProps) {
  const [searchParams, setSearchParams] = useSearchParams()
  const [category, setCategory] = useState<Category>('전체')
  const [sort, setSort] = useState<'popular' | 'latest'>('popular')
  const query = searchParams.get('q') ?? ''

  const popularApps = useMemo(() => [...apps].sort((a, b) => b.likes - a.likes || b.createdAt.localeCompare(a.createdAt)).slice(0, 4), [apps])
  const filteredApps = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()
    return [...apps]
      .filter((app) => category === '전체' || app.category === category)
      .filter((app) => {
        if (!normalizedQuery) return true
        return [app.name, app.tagline, app.maker.name].some((value) => value.toLowerCase().includes(normalizedQuery))
      })
      .sort((a, b) => sort === 'popular' ? b.likes - a.likes || b.createdAt.localeCompare(a.createdAt) : b.createdAt.localeCompare(a.createdAt))
  }, [apps, category, query, sort])

  const clearFilters = () => {
    setCategory('전체')
    setSearchParams({})
  }

  return (
    <div className="home-page">
      <DropZone apps={apps} />

      <section className="home-section home-section--featured">
        <div className="content-container">
          <div className="section-heading">
            <div>
              <span className="section-kicker">추천 서비스</span>
              <h2>실시간 순위</h2>
            </div>
            <span className="section-heading__note">좋아요가 많은 순서입니다.</span>
          </div>
          {popularApps.length > 0 ? <div className="featured-grid">
            {popularApps.map((app, index) => (
              <AppCard key={app.id} app={app} isBookmarked={bookmarkedIds.includes(app.id)} onToggleBookmark={onToggleBookmark} featured={index === 0} />
            ))}
          </div> : <EmptyState type="catalog" />}
        </div>
      </section>

      <section className="home-section home-section--all" id="all-apps">
        <div className="content-container">
          <div className="section-heading section-heading--list">
            <div>
              <span className="section-kicker">카테고리</span>
              <h2>{query ? `“${query}” 검색 결과` : '전체 서비스'}</h2>
            </div>
            <div className="sort-control" aria-label="앱 정렬">
              <button type="button" className={sort === 'popular' ? 'is-selected' : ''} onClick={() => setSort('popular')}>인기순</button>
              <span>/</span>
              <button type="button" className={sort === 'latest' ? 'is-selected' : ''} onClick={() => setSort('latest')}>최신순</button>
            </div>
          </div>
          <div className="category-strip" aria-label="카테고리 필터">
            <div className="category-strip__inner">
              <div className="category-list">
                {CATEGORIES.map((item) => (
                  <button type="button" key={item} className={`category-chip ${category === item ? 'is-selected' : ''}`} onClick={() => setCategory(item)} aria-pressed={category === item}>
                    {item}
                  </button>
                ))}
              </div>
              <span className="category-count">{formatNumber(filteredApps.length)}개의 앱</span>
            </div>
          </div>
          {filteredApps.length > 0 ? (
            <div className="app-grid">
              {filteredApps.map((app) => <AppCard key={app.id} app={app} isBookmarked={bookmarkedIds.includes(app.id)} onToggleBookmark={onToggleBookmark} />)}
            </div>
          ) : (
            <EmptyState type={query ? 'search' : 'catalog'} onReset={query ? clearFilters : undefined} />
          )}
          <div className="home-section__tail"><Link to="/submit" className="text-link">내 앱 올리기 <ArrowRight size={15} /></Link></div>
        </div>
      </section>
    </div>
  )
}
