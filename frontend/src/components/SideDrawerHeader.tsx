import { IconX } from '@tabler/icons-react';

import { DrawerClose, DrawerTitle } from '@/components/ui/drawer';

/** 옆 서랍 머리. 제목과 닫기 버튼 */
export function SideDrawerHeader({ title }: { title: string }) {
  return (
    <div className="flex h-14 shrink-0 items-center justify-between pr-4 pl-5">
      <DrawerTitle className="text-[0.9375rem] font-bold">{title}</DrawerTitle>
      <DrawerClose
        aria-label={`${title} 닫기`}
        className="focus-visible:outline-primary-600 -mr-2 inline-flex size-10 cursor-pointer items-center justify-center rounded-md text-gray-900 focus-visible:outline-2"
      >
        <IconX className="size-5.5" aria-hidden="true" />
      </DrawerClose>
    </div>
  );
}

/** 서랍 안 한 줄 링크·버튼 모양. 아이콘 20px, 글자 16px */
export const SIDE_DRAWER_ITEM =
  'focus-visible:outline-primary-600 flex w-full cursor-pointer items-center gap-3.5 px-5 py-3 text-left text-base font-medium text-gray-600 hover:bg-gray-50 focus-visible:outline-2 focus-visible:-outline-offset-2';
