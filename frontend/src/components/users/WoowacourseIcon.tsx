import { cn } from '@/utils/cn';
import { useEffect, useId, useRef, useState } from 'react';
import { createPortal } from 'react-dom';

/** 작은 크기에서도 식별되는 행성과 궤도 모티프의 소속 아이콘. */
export function WoowacourseIcon({
  userType,
  className,
}: {
  userType?: string | null;
  className?: string;
}) {
  const id = useId();
  const ref = useRef<HTMLSpanElement>(null);
  const touchRef = useRef(false);
  const [position, setPosition] = useState<{ left: number; top: number } | null>(null);
  const [portalHost, setPortalHost] = useState<HTMLElement | null>(null);
  const show = () => {
    setPortalHost(ref.current?.closest('dialog') ?? document.body);
    const rect = ref.current?.getBoundingClientRect();
    if (rect)
      setPosition({
        left: Math.max(12, Math.min(rect.left, window.innerWidth - 236)),
        top: Math.min(rect.bottom + 8, window.innerHeight - 68),
      });
  };
  useEffect(() => {
    if (!position) return;
    const close = () => setPosition(null);
    const outside = (event: PointerEvent) => {
      if (!ref.current?.contains(event.target as Node)) close();
    };
    const escape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        event.stopPropagation();
        close();
      }
    };
    document.addEventListener('pointerdown', outside);
    document.addEventListener('keydown', escape, true);
    window.addEventListener('scroll', close, true);
    window.addEventListener('resize', close);
    return () => {
      document.removeEventListener('pointerdown', outside);
      document.removeEventListener('keydown', escape, true);
      window.removeEventListener('scroll', close, true);
      window.removeEventListener('resize', close);
    };
  }, [position]);
  if (userType !== 'WOOWACOURSE_CREW' && userType !== 'WOOWACOURSE_COACH') return null;

  return (
    <>
      <span
        ref={ref}
        role="button"
        tabIndex={0}
        aria-describedby={position ? id : undefined}
        onPointerDown={(event) => {
          touchRef.current = event.pointerType === 'touch' || event.pointerType === 'pen';
          if (touchRef.current) event.preventDefault();
        }}
        onPointerEnter={(event) => {
          if (event.pointerType === 'mouse') {
            touchRef.current = false;
            show();
          }
        }}
        onPointerLeave={(event) => {
          if (event.pointerType === 'mouse') setPosition(null);
        }}
        onFocus={() => {
          if (!touchRef.current) show();
        }}
        onBlur={() => {
          touchRef.current = false;
          setPosition(null);
        }}
        onClick={(event) => {
          event.preventDefault();
          event.stopPropagation();
          if (touchRef.current && position) setPosition(null);
          else show();
        }}
        onKeyDown={(event) => {
          if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            event.stopPropagation();
            show();
          }
        }}
        aria-label="우아한테크코스 소속"
        className={cn(
          'focus-visible:outline-primary-600 relative z-20 -m-1 inline-flex size-6 shrink-0 cursor-help items-center justify-center rounded-sm align-text-bottom text-gray-600 focus-visible:outline-2',
          className,
        )}
      >
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.6"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
          className={cn('pointer-events-none size-4', className)}
        >
          <path d="M5.2 12.7a7 7 0 0 1 12.9-5.1M18.8 11.3a7 7 0 0 1-12.9 5.1" />
          <path
            d="M6 9.4C2.8 11.7 1.4 14 2.2 15.2c1.1 1.8 6.4.5 11.8-2.8s8.8-7.4 7.7-9.2c-.8-1.2-3.4-.9-6.5.4"
            transform="translate(0 2)"
          />
          <circle cx="10" cy="8" r=".8" fill="currentColor" stroke="none" />
          <circle cx="14" cy="7" r=".6" fill="currentColor" stroke="none" />
        </svg>
      </span>
      {position &&
        createPortal(
          <span
            id={id}
            role="tooltip"
            className="pointer-events-none fixed z-50 w-56 rounded-lg bg-gray-900 px-3 py-2 text-xs leading-5 font-normal text-white shadow-lg"
            style={position}
          >
            우아한테크코스 소속 인증을 완료한 사용자예요.
          </span>,
          portalHost ?? document.body,
        )}
    </>
  );
}
