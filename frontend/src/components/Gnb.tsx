import { useState, type ComponentProps, type ReactNode } from 'react';
import { Link, linkOptions, useLocation } from '@tanstack/react-router';
import {
  IconBell,
  IconMenu2,
  IconMessageCircle,
  IconNotebook,
  IconSearch,
  type Icon,
} from '@tabler/icons-react';

import { SIDE_DRAWER_ITEM, SideDrawerHeader } from '@/components/SideDrawerHeader';
import { tabItemBaseStyle, tabItemStyles } from '@/components/Tab';
import { Drawer, DrawerSideContent } from '@/components/ui/drawer';
import { cn } from '@/utils/cn';
import { analytics, toPathPattern, type NavTab } from '@/utils/analytics';

export const GNB_ITEMS = linkOptions([
  { to: '/', label: '탐색' },
  { to: '/community', label: '커뮤니티' },
  { to: '/magazines', label: '매거진' },
  { to: '/news', label: '소식' },
]);

/** 모바일 메뉴 서랍에서 항목 앞에 두는 아이콘 */
const ITEM_ICONS: Record<(typeof GNB_ITEMS)[number]['to'], Icon> = {
  '/': IconSearch,
  '/community': IconMessageCircle,
  '/magazines': IconNotebook,
  '/news': IconBell,
};

const NAV_TAB_BY_PATH: Record<string, NavTab> = {
  '/': 'explore',
  '/community': 'feeds',
  '/magazines': 'magazines',
  '/news': 'news',
};

/**
 * 링크 경로와 달라도 함께 활성으로 보여 줄 경로.
 * 프로토타입에서 프로젝트·채용공고·자료 정리 보기는 탐색 안의 화면이라 탐색이 활성이다.
 */
const EXTRA_ACTIVE_PATHS: Partial<Record<string, string[]>> = {
  '/': ['/projects', '/job-postings', '/materials'],
};

const isUnder = (pathname: string, path: string) =>
  pathname === path || pathname.startsWith(`${path}/`);

const isActive = (pathname: string, to: string) =>
  isUnder(pathname, to) ||
  (EXTRA_ACTIVE_PATHS[to]?.some((path) => isUnder(pathname, path)) ?? false);

const trackNavClick = (to: string) =>
  analytics.track({
    name: 'nav_tab_clicked',
    tab: NAV_TAB_BY_PATH[to]!,
    from: toPathPattern(window.location.pathname),
  });

export interface GnbProps extends ComponentProps<'header'> {
  trailing?: ReactNode;
}

/** 데스크톱 메뉴. 밑줄 없이 굵기와 색으로만 활성을 나타낸다. */
const NAV_ITEM = cn(
  tabItemBaseStyle,
  tabItemStyles.underline,
  'border-b-0 py-0 text-base data-[status=active]:font-bold',
);

/**
 * 데스크톱은 로고 · 가운데 메뉴 · 내 계정 한 줄(64px)이다.
 * 모바일은 ☰ · 로고 · 내 계정 한 줄(56px)이고, 메뉴는 ☰을 누르면 왼쪽 서랍으로 연다.
 */
export function Gnb({ trailing, className, ...props }: GnbProps) {
  const pathname = useLocation({ select: (location) => location.pathname });

  return (
    <header className={cn('bg-background sticky top-0 z-50', className)} {...props}>
      <div className="mx-auto flex h-14 max-w-6xl items-center justify-between gap-3 px-4 md:grid md:h-16 md:grid-cols-[1fr_auto_1fr]">
        <div className="flex items-center gap-1.5 md:gap-0">
          <MobileMenu pathname={pathname} />
          <Link to="/" className="flex items-center gap-[7px] md:gap-2">
            {/* 파비콘과 같은 아이콘을 쓴다. */}
            <img
              src="/favicon/android-chrome-192x192.png"
              alt=""
              width={28}
              height={28}
              className="size-6 md:size-7"
            />
            {/* DESIGN-TOKEN: 모바일 로고 글자는 프로토타입대로 17px이다. */}
            <span className="text-[1.0625rem] font-extrabold tracking-tight text-gray-900 md:text-lg">
              shout-outz
            </span>
          </Link>
        </div>

        <nav aria-label="서비스 메뉴" className="hidden items-center gap-9 md:flex">
          {GNB_ITEMS.map((item) => {
            const active = isActive(pathname, item.to);
            return (
              <Link
                key={item.to}
                to={item.to}
                data-status={active ? 'active' : 'inactive'}
                aria-current={active ? 'page' : undefined}
                className={NAV_ITEM}
                onClick={() => trackNavClick(item.to)}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="flex items-center justify-end">{trailing}</div>
      </div>
    </header>
  );
}

/** 모바일 ☰ 버튼과 왼쪽 메뉴 서랍 */
function MobileMenu({ pathname }: { pathname: string }) {
  const [open, setOpen] = useState(false);

  return (
    <Drawer direction="left" open={open} onOpenChange={setOpen}>
      <button
        type="button"
        aria-label="메뉴 열기"
        aria-expanded={open}
        aria-controls="mobile-menu"
        onClick={() => setOpen(true)}
        className="focus-visible:outline-primary-600 -ml-2 inline-flex size-10 cursor-pointer items-center justify-center rounded-md text-gray-900 focus-visible:outline-2 md:hidden"
      >
        <IconMenu2 className="size-5.5" aria-hidden="true" />
      </button>
      <DrawerSideContent side="left" id="mobile-menu" aria-describedby={undefined}>
        <SideDrawerHeader title="메뉴" />
        <nav aria-label="서비스 메뉴" className="flex-1 overflow-auto pt-1 pb-4">
          {GNB_ITEMS.map((item) => {
            const active = isActive(pathname, item.to);
            const ItemIcon = ITEM_ICONS[item.to];
            return (
              <Link
                key={item.to}
                to={item.to}
                aria-current={active ? 'page' : undefined}
                className={cn(SIDE_DRAWER_ITEM, active && 'bg-gray-50 font-bold text-gray-900')}
                onClick={() => {
                  trackNavClick(item.to);
                  setOpen(false);
                }}
              >
                <ItemIcon className="size-5 shrink-0" aria-hidden="true" />
                {item.label}
              </Link>
            );
          })}
        </nav>
      </DrawerSideContent>
    </Drawer>
  );
}
