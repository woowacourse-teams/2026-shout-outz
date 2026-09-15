import { Link } from '@tanstack/react-router';

const LINKS = [
  { to: '/projects', label: '프로젝트 탐색' },
  { to: '/feeds', label: '피드' },
  { to: '/news', label: '소식' },
] as const;

export function FeedFooter() {
  return (
    <footer className="border-t border-gray-200 bg-gray-50">
      <div className="mx-auto flex max-w-6xl flex-col gap-5 px-4 py-8 md:flex-row md:justify-between md:px-16 md:py-12">
        <div>
          <p className="text-lg font-bold">shout-outz</p>
          <p className="mt-3 text-sm text-gray-500">우아한테크코스 아카이빙 &amp; 소셜 플랫폼</p>
        </div>
        <nav aria-label="서비스 바로가기">
          <p className="mb-3 hidden text-sm font-semibold md:block">서비스 바로가기</p>
          <div className="flex gap-4 text-sm text-gray-500">
            {LINKS.map((link) => (
              <Link key={link.to} to={link.to} className="hover:text-gray-900">
                {link.label}
              </Link>
            ))}
          </div>
        </nav>
      </div>
      <p className="mx-auto max-w-6xl px-4 pb-8 text-xs text-gray-400 md:px-16 md:pb-12">
        © 2026 shout-outz. All rights reserved.
      </p>
    </footer>
  );
}
