import { ArrowLeft, Compass } from 'lucide-react'
import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return <div className="not-found page-pad"><div className="not-found__icon"><Compass size={28} /></div><span className="section-kicker">404 · 페이지 없음</span><h1>페이지를 찾을 수 없습니다.</h1><p>주소를 확인하거나 홈으로 돌아가세요.</p><Link to="/" className="button button--primary"><ArrowLeft size={16} /> 홈으로 돌아가기</Link></div>
}
