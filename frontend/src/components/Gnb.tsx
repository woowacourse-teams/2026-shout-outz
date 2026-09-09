import { Link, linkOptions } from '@tanstack/react-router';
import type { ComponentProps, ReactNode } from 'react';

import { tabItemBaseStyle, tabItemSizeStyles, tabItemStyles } from '@/components/Tab';
import { cn } from '@/utils/cn';

export const GNB_ITEMS = linkOptions([
  { to: '/', activeOptions: { exact: true }, label: '홈' },
  { to: '/feeds', label: '피드' },
  { to: '/projects', label: '프로젝트' },
  { to: '/news', label: '소식' },
]);

export interface GnbProps extends ComponentProps<'header'> {
  trailing?: ReactNode;
}

const NAV_ITEM = [
  tabItemBaseStyle,
  tabItemSizeStyles.sm,
  tabItemStyles.underline,
  'md:rounded-lg md:border-0 md:bg-transparent md:text-gray-600',
  'md:data-[status=active]:bg-primary-50 md:data-[status=active]:text-primary-600',
].join(' ');

export function Gnb({ trailing, className, ...props }: GnbProps) {
  return (
    <header
      className={cn(
        'bg-background sticky top-0 z-50 border-b border-gray-100 md:border-gray-200',
        className,
      )}
      {...props}
    >
      <div className="mx-auto flex max-w-[1140px] flex-wrap items-center px-4 md:px-16">
        <Link to="/" className="flex h-14 items-center gap-2 md:h-18 md:gap-2.5">
          <span
            className="bg-primary-600 flex size-7 items-center justify-center rounded-lg text-sm font-bold text-white md:size-8 md:text-base"
            aria-hidden="true"
          >
            S
          </span>
          <span className="text-lg font-bold text-gray-900 md:text-xl">shout-outz</span>
        </Link>

        <nav className="order-last flex h-11 w-full items-center gap-4 border-t border-gray-100 md:order-none md:ml-9 md:h-18 md:w-auto md:gap-2 md:border-t-0">
          {GNB_ITEMS.map((item) => (
            <Link key={item.to} {...item} className={NAV_ITEM}>
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="ml-auto flex h-14 items-center md:h-18">{trailing}</div>
      </div>
    </header>
  );
}
