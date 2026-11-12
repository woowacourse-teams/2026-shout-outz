import { useLayoutEffect, useRef, type ReactNode } from 'react';

import { cn } from '@/utils/cn';

export interface ModalProps {
  children: ReactNode;
  onClose: () => void;
  className?: string;
  /** 모달의 목적을 알리는 이름입니다. 화면의 제목이 목적을 설명하지 못할 때 `labelledBy` 대신 씁니다. */
  label?: string;
  labelledBy?: string;
  describedBy?: string;
}

export function Modal({
  children,
  onClose,
  className,
  label,
  labelledBy,
  describedBy,
}: ModalProps) {
  const ref = useRef<HTMLDialogElement>(null);

  // 닫을 때 브라우저가 연 버튼으로 포커스를 돌려주는 것은 dialog가 문서에 붙어 있을 때만이다.
  // useEffect의 정리는 React가 dialog를 DOM에서 뺀 뒤에 실행되므로, 빼기 전에 실행되는 useLayoutEffect로 닫는다.
  useLayoutEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;

    dialog.showModal();
    dialog.querySelector<HTMLElement>('[data-modal-autofocus]')?.focus();
    const { overflow } = document.body.style;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = overflow;
      if (dialog.open) dialog.close();
    };
  }, []);

  return (
    <dialog
      ref={ref}
      aria-label={labelledBy ? undefined : label}
      aria-labelledby={labelledBy}
      aria-describedby={describedBy}
      onClose={onClose}
      onCancel={onClose}
      onClick={(event) => {
        if (event.target === ref.current) onClose();
      }}
      className={cn(
        'bg-transparent p-0 backdrop:bg-gray-950/40',
        'm-0 mt-auto max-h-dvh w-full max-w-full',
        'md:m-auto md:w-fit md:max-w-[90vw]',
      )}
    >
      <div
        className={cn(
          'bg-background flex max-h-[85dvh] w-full flex-col overflow-hidden text-gray-900',
          'rounded-t-4xl',
          'md:max-h-[80dvh] md:rounded-2xl',
          className,
        )}
      >
        {children}
      </div>
    </dialog>
  );
}
