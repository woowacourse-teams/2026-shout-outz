import { Link, linkOptions, useLocation } from '@tanstack/react-router';
import type { ComponentProps, ReactNode } from 'react';

import { tabItemBaseStyle, tabItemStyles } from '@/components/Tab';
import { cn } from '@/utils/cn';
import { analytics, toPathPattern, type NavTab } from '@/utils/analytics';

export const GNB_ITEMS = linkOptions([
  { to: '/', activeOptions: { exact: true }, label: '탐색' },
  { to: '/community', label: '커뮤니티' },
  { to: '/magazines', label: '매거진' },
  { to: '/news', label: '소식' },
]);

const NAV_TAB_BY_PATH: Record<string, NavTab> = {
  '/': 'home',
  '/community': 'feeds',
  '/magazines': 'magazines',
  '/news': 'news',
};

/**
 * 링크 경로와 달라도 함께 활성으로 보여 줄 경로.
 * 프로토타입에서 프로젝트는 탐색 안의 탭이고, 프로젝트 상세에서도 탐색이 활성이다.
 */
const EXTRA_ACTIVE_PATHS: Partial<Record<string, string[]>> = {
  '/': ['/projects'],
};

const isUnder = (pathname: string, path: string) =>
  pathname === path || pathname.startsWith(`${path}/`);

export interface GnbProps extends ComponentProps<'header'> {
  trailing?: ReactNode;
}

/** 모바일은 Tab의 underline variant, 데스크톱은 밑줄 없이 굵기와 색으로만 활성을 나타낸다. */
const NAV_ITEM = cn(
  tabItemBaseStyle,
  tabItemStyles.underline,
  '-mb-px px-0 py-3 text-sm data-[status=active]:font-bold',
  'md:mb-0 md:border-b-0 md:py-0 md:text-base',
);

export function Gnb({ trailing, className, ...props }: GnbProps) {
  const pathname = useLocation({ select: (location) => location.pathname });

  return (
    <header className={cn('bg-background sticky top-0 z-50', className)} {...props}>
      <div className="mx-auto flex max-w-6xl flex-wrap items-center md:flex-nowrap md:px-4">
        <div className="flex h-14 flex-1 items-center pl-4 md:h-16 md:pl-0">
          <Link to="/" className="flex items-center gap-2">
            {/* 파비콘과 같은 아이콘을 쓴다. */}
            <img
              src="/favicon/android-chrome-192x192.png"
              alt=""
              width={28}
              height={28}
              className="size-7"
            />
            <span className="text-lg font-extrabold tracking-tight text-gray-900">shout-outz</span>
          </Link>
        </div>

        <nav className="order-last flex w-full items-center gap-6 border-b border-gray-200 px-4 md:order-none md:w-auto md:gap-9 md:border-b-0 md:px-0">
          {GNB_ITEMS.map((item) => {
            const extraActive = EXTRA_ACTIVE_PATHS[item.to]?.some((path) =>
              isUnder(pathname, path),
            );

            return (
              <Link
                key={item.to}
                {...item}
                {...(extraActive && { 'data-status': 'active', 'aria-current': 'page' })}
                className={NAV_ITEM}
                onClick={() =>
                  analytics.track({
                    name: 'nav_tab_clicked',
                    tab: NAV_TAB_BY_PATH[item.to]!,
                    from: toPathPattern(window.location.pathname),
                  })
                }
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="flex h-14 flex-1 items-center justify-end pr-4 md:h-16 md:pr-0">
          {trailing}
        </div>
      </div>
    </header>
  );
}
