import { Link } from '@tanstack/react-router';

const FOOTER_LINKS = [
  { to: '/about', label: '서비스 소개' },
  { to: '/explore', label: '탐색' },
  { to: '/community', label: '커뮤니티' },
  { to: '/magazines', label: '매거진' },
  { to: '/news', label: '소식' },
] as const;

export function Footer({ linksHref }: { linksHref?: string } = {}) {
  return (
    <footer className="border-t border-gray-200 bg-gray-50">
      <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-8 md:flex-row md:justify-between">
        <div>
          <p className="text-lg font-bold">shout-outz</p>
          <p className="mt-2 text-sm text-gray-500">IT 취준생을 위한, 취업 커뮤니티</p>
        </div>
        <nav aria-label="서비스 바로가기" className="flex items-center">
          <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-gray-500">
            {FOOTER_LINKS.map((link) =>
              linksHref ? (
                <a
                  key={link.to}
                  href={linksHref}
                  className="focus-visible:outline-primary-600 rounded-sm hover:text-gray-900 focus-visible:outline-2"
                >
                  {link.label}
                </a>
              ) : (
                <Link
                  key={link.to}
                  to={link.to}
                  className="focus-visible:outline-primary-600 rounded-sm hover:text-gray-900 focus-visible:outline-2"
                >
                  {link.label}
                </Link>
              ),
            )}
          </div>
        </nav>
      </div>
      <p className="mx-auto max-w-6xl px-4 pb-8 text-xs text-gray-400">
        © 2026 shout-outz. All rights reserved.
      </p>
    </footer>
  );
}
