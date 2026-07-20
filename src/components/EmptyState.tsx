import { ArrowRight, BookmarkX, SearchX } from 'lucide-react'
import { Link } from 'react-router-dom'

export function EmptyState({ type = 'bookmark', onReset }: { type?: 'bookmark' | 'search' | 'app' | 'catalog'; onReset?: () => void }) {
  const isSearch = type === 'search'
  const isApp = type === 'app'
  const isCatalog = type === 'catalog'
  return (
    <div className="empty-state">
      <span className="empty-state__icon">{isSearch || isApp ? <SearchX size={22} /> : <BookmarkX size={22} />}</span>
      <h2>{isSearch ? '검색 결과가 없습니다.' : isApp ? '앱을 찾을 수 없습니다.' : isCatalog ? '등록된 앱이 없습니다.' : '저장한 앱이 없습니다.'}</h2>
      <p>{isSearch ? '검색어를 바꿔보거나 전체 목록을 확인하세요.' : isApp ? '주소를 확인하거나 전체 앱 목록으로 돌아가세요.' : isCatalog ? '앱을 등록하면 여기에 표시됩니다.' : '저장한 앱이 여기에 표시됩니다.'}</p>
      {isSearch && onReset ? (
        <button type="button" className="button button--secondary" onClick={onReset}>검색 초기화 <ArrowRight size={15} /></button>
      ) : isApp && onReset ? (
        <button type="button" className="button button--secondary" onClick={onReset}>앱 목록으로 돌아가기 <ArrowRight size={15} /></button>
      ) : isCatalog ? (
        <Link to="/submit" className="button button--primary">첫 앱 등록하기 <ArrowRight size={15} /></Link>
      ) : (
        <Link to="/" className="button button--primary">앱 둘러보기 <ArrowRight size={15} /></Link>
      )}
    </div>
  )
}
