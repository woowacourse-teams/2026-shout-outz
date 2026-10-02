import { type ReactNode } from 'react';
import { IconX } from '@tabler/icons-react';

import { Modal } from '@/components/Modal';
import { Button } from '@/components/Button';
import { cn } from '@/utils/cn';

export interface SelectionModalProps {
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
  width?: 'md' | 'lg';
}

const WIDTHS = {
  md: 'md:w-145', // 580px
  lg: 'md:w-155', // 620px
} as const;

export function SelectionModal({
  title,
  onClose,
  children,
  footer,
  width = 'md',
}: SelectionModalProps) {
  return (
    <Modal onClose={onClose} className={WIDTHS[width]}>
      <div aria-hidden="true" className="flex justify-center pt-4 pb-1 md:hidden">
        <span className="h-1 w-9 rounded-full bg-gray-300" />
      </div>

      <header className="flex items-center justify-between gap-4 px-4 pt-3 pb-5 md:px-6 md:py-5.5">
        <h2 className="text-lg font-bold">{title}</h2>
        <Button
          variant="ghost"
          size="sm"
          aria-label="닫기"
          onClick={onClose}
          className="size-7.5 rounded-full bg-gray-100 p-0 md:size-auto md:bg-transparent"
        >
          <IconX className="size-4" aria-hidden="true" />
        </Button>
      </header>

      <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto px-4 pb-4 md:px-6 md:pb-5">
        {children}
      </div>

      {footer && (
        <footer className="flex items-center justify-between gap-4 px-4 pt-2 pb-7 md:px-6 md:py-4">
          {footer}
        </footer>
      )}
    </Modal>
  );
}

/**
 * 모달 안의 검색 입력. 디자인의 회색 라운드 박스다.
 *
 * `showModal()`은 기본적으로 첫 포커스 대상(머리말의 닫기 버튼)에 포커스를 준다. 검색으로 시작하는
 * 모달에서는 열자마자 바로 칠 수 있어야 해서, `Modal`이 열린 뒤 찾아가도록 표시만 남긴다.
 */
export function ModalSearchInput({
  value,
  onChange,
  label,
  placeholder,
  autoFocus,
}: {
  value: string;
  onChange: (next: string) => void;
  label: string;
  placeholder: string;
  autoFocus?: boolean;
}) {
  return (
    <input
      type="search"
      data-modal-autofocus={autoFocus ? '' : undefined}
      aria-label={label}
      value={value}
      onChange={(event) => onChange(event.target.value)}
      placeholder={placeholder}
      className="focus-visible:outline-primary-600 h-10 w-full shrink-0 rounded-xl bg-gray-100 px-4 text-sm text-gray-900 outline-none placeholder:text-gray-500 focus-visible:outline-2 md:h-10.5 md:rounded-lg"
    />
  );
}

/** 지금 고른 것을 모아 보여주는 상자. 칩을 누르면 빠진다. */
export function SelectedCollector<T>({
  label,
  items,
  getKey,
  getLabel,
  onRemove,
  onClear,
}: {
  label: string;
  items: T[];
  getKey: (item: T) => string | number;
  getLabel: (item: T) => string;
  onRemove: (item: T) => void;
  onClear?: () => void;
}) {
  if (items.length === 0) return null;

  return (
    <section aria-label={label} className="flex shrink-0 flex-col gap-2 rounded-xl bg-gray-50 p-3">
      <div className="flex items-center justify-between gap-2">
        <p className="text-xs font-bold text-gray-700">{`${label} (${items.length}개)`}</p>
        {onClear && (
          <button
            type="button"
            onClick={onClear}
            className="focus-visible:outline-primary-600 cursor-pointer rounded-sm text-xs text-gray-500 hover:text-gray-700 focus-visible:outline-2"
          >
            모두 지우기
          </button>
        )}
      </div>
      <ul className="flex flex-wrap gap-1.5">
        {items.map((item) => (
          <li key={getKey(item)}>
            <button
              type="button"
              onClick={() => onRemove(item)}
              aria-label={`${getLabel(item)} 선택 해제`}
              className="bg-primary-50 text-primary-600 focus-visible:outline-primary-600 flex cursor-pointer items-center gap-1 rounded-lg px-2 py-1 text-xs font-bold focus-visible:outline-2"
            >
              {getLabel(item)}
              <IconX className="size-3" aria-hidden="true" />
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}

/**
 * 고를 수 있는 항목 한 칸.
 *
 * 고른 것은 파란 배경에 파란 테두리와 체크, 고르지 않은 것은 회색 테두리다. 두 상태 모두 1px
 * 테두리를 둬서 토글할 때 칸 크기가 흔들리지 않게 한다.
 *
 * 체크 자리는 디자인이 화면마다 다르다. PC 모달(🖥️ 15·16)은 이름 앞에, 모바일 시트(📱 6·14-S)는
 * 오른쪽 끝에 둔다. 두 자리를 다 그려 두고 `md:`로 보이는 쪽만 바꾼다. 앞자리는 고르지 않았을 때도
 * 투명한 채로 자리를 지켜, 토글할 때 이름이 밀리지 않는다.
 * 상태는 `role="checkbox"`와 `aria-checked`가 전하므로 두 체크 모두 읽히지 않게 숨긴다.
 */
export function SelectableItem({
  selected,
  onClick,
  children,
  meta,
}: {
  selected: boolean;
  onClick: () => void;
  children: ReactNode;
  meta?: ReactNode;
}) {
  return (
    <button
      type="button"
      role="checkbox"
      aria-checked={selected}
      onClick={onClick}
      className={cn(
        'focus-visible:outline-primary-600 flex w-full cursor-pointer items-center justify-between gap-2 rounded-xl border px-4 py-3 text-left text-sm focus-visible:outline-2 md:rounded-lg md:py-2.5',
        selected
          ? 'border-primary-600 bg-primary-50 text-primary-600 font-bold'
          : 'border-gray-200 bg-gray-50 text-gray-900 hover:bg-gray-100 md:bg-transparent md:hover:bg-gray-50',
      )}
    >
      <span
        aria-hidden="true"
        className={cn('hidden shrink-0 md:inline', !selected && 'text-transparent')}
      >
        ✓
      </span>
      <span className="min-w-0 flex-1 truncate">{children}</span>
      <span className="flex shrink-0 items-center gap-1 text-xs">
        {meta}
        {selected && (
          <span aria-hidden="true" className="md:hidden">
            ✓
          </span>
        )}
      </span>
    </button>
  );
}
