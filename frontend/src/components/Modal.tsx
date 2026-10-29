import { useEffect, useRef, type ReactNode } from 'react';

import { cn } from '@/utils/cn';

export interface ModalProps {
  children: ReactNode;
  onClose: () => void;
  className?: string;
  labelledBy?: string;
  describedBy?: string;
}

export function Modal({ children, onClose, className, labelledBy, describedBy }: ModalProps) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
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
