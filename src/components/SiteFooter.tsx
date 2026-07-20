import { ArrowUpRight, Code2 } from 'lucide-react'
import { Link } from 'react-router-dom'

export function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="site-footer__top">
        <div>
          <Link to="/" className="footer-brand"><img src="/dropit-icon.svg" alt="" className="footer-brand__icon" /><span className="footer-brand__word">dropit<span>.</span></span></Link>
          <p>내가 만든 서비스를 손쉽게 공유하세요.</p>
        </div>
        <div className="site-footer__links">
          <div>
            <strong>둘러보기</strong>
            <Link to="/">전체 서비스</Link>
            <Link to="/bookmarks">저장한 앱</Link>
          </div>
          <div>
            <strong>만들기</strong>
            <Link to="/submit">앱 등록하기</Link>
            <a href="https://github.com/sangjun121/dropit" target="_blank" rel="noreferrer"><Code2 size={14} /> GitHub <ArrowUpRight size={12} /></a>
          </div>
        </div>
      </div>
      <div className="site-footer__bottom">
        <span>Dropit 2026</span>
        <span>우테코 크루가 만든 서비스 모음</span>
      </div>
    </footer>
  )
}
