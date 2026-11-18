import { IconChevronDown, IconChevronRight } from '@tabler/icons-react';
import type { ComponentPropsWithoutRef } from 'react';

interface ExploreMoreButtonProps extends Omit<ComponentPropsWithoutRef<'button'>, 'type'> {
  /** 목록을 아래로 이어 붙이면 down, 다른 보기로 넘어가면 right. 없으면 아이콘을 두지 않는다. */
  icon?: 'down' | 'right';
}

/** 탐색 화면(키워드·채용공고·프로젝트 탭) 목록 아래 가운데에 두는 알약 모양 더 보기 버튼 */
export function ExploreMoreButton({ icon, children, ...props }: ExploreMoreButtonProps) {
  const Icon = icon === 'down' ? IconChevronDown : icon === 'right' ? IconChevronRight : null;
  return (
    <div className="mt-8 flex justify-center">
      <button
        type="button"
        className="focus-visible:outline-primary-600 inline-flex h-8.5 cursor-pointer items-center gap-1 rounded-full border border-gray-200 px-3.5 text-sm font-semibold text-gray-600 hover:bg-gray-50 focus-visible:outline-2 disabled:cursor-default disabled:opacity-50"
        {...props}
      >
        {children}
        {Icon && <Icon className="size-3.5" aria-hidden="true" />}
      </button>
    </div>
  );
}
