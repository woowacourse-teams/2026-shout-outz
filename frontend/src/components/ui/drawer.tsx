import type { ComponentProps } from 'react';
import { Drawer as DrawerPrimitive } from 'vaul';

import { cn } from '@/utils/cn';

// shadcn/ui Drawer(new-york-v4)를 기반으로 색상을 프로젝트 컬러 토큰에 연결했다.
// 아래에서 올라오는 바텀시트(DrawerContent)와 GNB 모바일 메뉴가 쓰는 옆 서랍(DrawerSideContent)만 둔다.

function Drawer(props: ComponentProps<typeof DrawerPrimitive.Root>) {
  return <DrawerPrimitive.Root data-slot="drawer" {...props} />;
}

function DrawerPortal(props: ComponentProps<typeof DrawerPrimitive.Portal>) {
  return <DrawerPrimitive.Portal data-slot="drawer-portal" {...props} />;
}

function DrawerClose(props: ComponentProps<typeof DrawerPrimitive.Close>) {
  return <DrawerPrimitive.Close data-slot="drawer-close" {...props} />;
}

function DrawerOverlay({ className, ...props }: ComponentProps<typeof DrawerPrimitive.Overlay>) {
  return (
    <DrawerPrimitive.Overlay
      data-slot="drawer-overlay"
      className={cn('bg-gray-opacity-400 fixed inset-0 z-50', className)}
      {...props}
    />
  );
}

function DrawerContent({
  className,
  children,
  ...props
}: ComponentProps<typeof DrawerPrimitive.Content>) {
  return (
    <DrawerPortal>
      <DrawerOverlay />
      <DrawerPrimitive.Content
        data-slot="drawer-content"
        className={cn(
          'bg-background fixed inset-x-0 bottom-0 z-50 mt-24 flex max-h-[80vh] flex-col rounded-t-2xl outline-none',
          className,
        )}
        {...props}
      >
        <div aria-hidden="true" className="flex shrink-0 justify-center pt-2">
          <span className="h-1 w-9 rounded-full bg-gray-300" />
        </div>
        {children}
      </DrawerPrimitive.Content>
    </DrawerPortal>
  );
}

/**
 * 화면 옆에서 밀려 나오는 300px 서랍. `Drawer`의 `direction`을 같은 쪽(left, right)으로 함께 넘긴다.
 * GNB 모바일의 메뉴(왼쪽)와 내 계정(오른쪽)이 쓴다.
 */
function DrawerSideContent({
  side,
  className,
  children,
  ...props
}: ComponentProps<typeof DrawerPrimitive.Content> & { side: 'left' | 'right' }) {
  return (
    <DrawerPortal>
      <DrawerOverlay />
      <DrawerPrimitive.Content
        data-slot="drawer-content"
        className={cn(
          // DESIGN-TOKEN: 프로토타입 서랍 너비 300px과 서랍 그림자는 토큰에 없는 값이다. 좁은 화면에서는 화면 끝 여백을 남긴다.
          'bg-background fixed inset-y-0 z-50 flex w-[300px] max-w-[85vw] flex-col outline-none',
          side === 'left'
            ? 'left-0 shadow-[12px_0_32px_-16px_rgba(15,23,42,0.35)]'
            : 'right-0 shadow-[-12px_0_32px_-16px_rgba(15,23,42,0.35)]',
          className,
        )}
        {...props}
      >
        {children}
      </DrawerPrimitive.Content>
    </DrawerPortal>
  );
}

function DrawerTitle({ className, ...props }: ComponentProps<typeof DrawerPrimitive.Title>) {
  return (
    <DrawerPrimitive.Title
      data-slot="drawer-title"
      className={cn('font-semibold text-gray-900', className)}
      {...props}
    />
  );
}

function DrawerDescription({
  className,
  ...props
}: ComponentProps<typeof DrawerPrimitive.Description>) {
  return (
    <DrawerPrimitive.Description
      data-slot="drawer-description"
      className={cn('text-sm text-gray-500', className)}
      {...props}
    />
  );
}

export {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerDescription,
  DrawerOverlay,
  DrawerPortal,
  DrawerSideContent,
  DrawerTitle,
};
