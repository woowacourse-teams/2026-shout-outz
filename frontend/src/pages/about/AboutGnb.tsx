import { AuthActions } from '@/components/auth/AuthActions';
import { GNB_ITEMS } from '@/components/Gnb';
import { tabItemBaseStyle, tabItemSizeStyles, tabItemStyles } from '@/components/Tab';
import { PROTOTYPE_URL } from '@/pages/about/links';
import { cn } from '@/utils/cn';

// 서비스 GNB(components/Gnb.tsx)와 같은 모양
const NAV_ITEM = cn(
  tabItemBaseStyle,
  tabItemSizeStyles.sm,
  tabItemStyles.underline,
  'md:rounded-lg md:border-0 md:bg-transparent md:text-gray-600',
  'md:data-[status=active]:bg-primary-50 md:data-[status=active]:text-primary-600',
);

// 서비스 버튼의 비활성 표현(disabled:opacity-50, cursor-not-allowed)과 맞춘다.
const LOCKED = 'cursor-not-allowed opacity-40';

/**
 * 소개 페이지 전용 GNB. 프로젝트 탭만 프로토타입으로 연결하고 나머지 탭, 로고 링크, 로그인·알림·계정 메뉴는 잠근다.
 * 소개 페이지에서 프로토타입 범위 밖의 화면으로 나가지 않게 한다.
 */
export function AboutGnb() {
  return (
    <header className="bg-background sticky top-0 z-50 border-b border-gray-100 md:border-gray-200">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center px-4">
        <div className="flex h-14 items-center gap-2 opacity-40 md:h-18 md:gap-2.5">
          {/* 파비콘과 같은 아이콘을 쓴다. */}
          <img
            src="/favicon/android-chrome-192x192.png"
            alt=""
            width={32}
            height={32}
            className="size-7 md:size-8"
          />
          <span className="text-lg font-bold text-gray-900 md:text-xl">샤라웃</span>
        </div>

        <nav
          aria-label="주요 메뉴"
          className="order-last flex h-11 w-full items-center gap-4 border-t border-gray-100 md:order-none md:ml-9 md:h-18 md:w-auto md:gap-2 md:border-t-0"
        >
          {GNB_ITEMS.map((item) =>
            item.to === '/projects' ? (
              <a
                key={item.to}
                href={PROTOTYPE_URL}
                className={cn(
                  NAV_ITEM,
                  'text-primary-700 hover:text-primary-800 md:text-primary-700',
                )}
                data-status="inactive"
              >
                {item.label}
              </a>
            ) : (
              <span
                key={item.to}
                aria-disabled="true"
                title="프로토타입 범위가 아니에요"
                className={cn(NAV_ITEM, 'cursor-not-allowed text-gray-400 md:text-gray-400')}
                data-status="inactive"
              >
                {item.label}
              </span>
            ),
          )}
        </nav>

        {/* 로그인, 알림, 계정 메뉴도 잠근다. 모양은 그대로 두고 비활성처럼 보이게 한다. */}
        <div
          aria-disabled="true"
          title="프로토타입 범위가 아니에요"
          className={cn('ml-auto flex h-14 items-center md:h-18', LOCKED)}
        >
          <div inert className="pointer-events-none">
            <AuthActions />
          </div>
        </div>
      </div>
    </header>
  );
}
