import type { ReactNode } from 'react';

import { cn } from '@/utils/cn';

interface DotTabProps {
  selected: boolean;
  /** 없으면 개수를 보여 주지 않는다. */
  count?: number;
  onSelect: () => void;
  children: ReactNode;
}

/** 고른 탭 앞에 점을 찍는 작은 탭. 자료 종류나 직군을 고를 때 쓴다. role="tablist" 안에 둔다. */
export function DotTab({ selected, count, onSelect, children }: DotTabProps) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={selected}
      onClick={onSelect}
      className={cn(
        'focus-visible:outline-primary-600 inline-flex cursor-pointer items-center gap-1.5 rounded-sm text-sm focus-visible:outline-2',
        selected ? 'font-semibold text-gray-900' : 'font-medium text-gray-400 hover:text-gray-700',
      )}
    >
      <span
        aria-hidden="true"
        className={cn('size-1.5 rounded-full', selected ? 'bg-gray-900' : 'bg-transparent')}
      />
      {children}
      {count !== undefined && <span className="font-medium text-gray-400">{count}</span>}
    </button>
  );
}
