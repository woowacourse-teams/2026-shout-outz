import { ArrowRight, Plus } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { AppItem } from '../types'
import { AppThumbnail } from './AppThumbnail'

export function DropZone({ apps }: { apps: AppItem[] }) {
  const recentApps = [...apps].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 5)
  const shouldAnimate = recentApps.length >= 3
  const railApps = shouldAnimate ? [...recentApps, ...recentApps] : recentApps
  return (
    <section className="drop-zone" aria-labelledby="drop-zone-title">
      <div className="drop-zone__copy">
        <h1 id="drop-zone-title">직접 만든 서비스를<br /><em>함께 공유해보세요.</em></h1>
        <p>우테코 크루들의 서비스를 구경해보세요.</p>
        <Link to="/submit" className="button button--primary">앱 등록하기 <ArrowRight size={16} /></Link>
      </div>
      <div className="drop-zone__rail" aria-label="최근 등록된 앱">
        <div className="drop-zone__rail-label"><span>최근에 올라온 서비스</span><span>{recentApps.length}개</span></div>
        {recentApps.length > 0 ? <div className={`drop-zone__cards ${shouldAnimate ? '' : 'is-static'}`}>
          {railApps.map((app, index) => (
            <Link to={`/apps/${app.id}`} className="drop-card" key={`${app.id}-${index}`} aria-label={`${app.name} 상세 보기`}>
              <AppThumbnail variant={app.thumbnailVariant} label={app.name} size="small" customImage={app.thumbnailUrl} />
              <div>
                <strong>{app.name}</strong>
                <span>{app.category} - {app.maker.name}</span>
              </div>
              <span className="drop-card__plus"><Plus size={14} /></span>
            </Link>
          ))}
        </div> : <div className="drop-zone__empty"><span><Plus size={18} /></span><strong>등록된 앱이 없습니다.</strong><small>앱을 등록하면 여기에 표시됩니다.</small></div>}
      </div>
    </section>
  )
}
