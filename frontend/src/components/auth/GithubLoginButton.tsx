import { useEffect, useRef, useState, type ReactNode } from 'react';
import { createPortal } from 'react-dom';

import { getGithubLoginUrl } from '@/utils/auth';
import { analytics, toPathPattern } from '@/utils/analytics';

interface GithubLoginButtonProps {
  children: ReactNode;
  className?: string;
  onBeforeLogin?: () => boolean;
}

export function GithubLoginButton({ children, className, onBeforeLogin }: GithubLoginButtonProps) {
  const locked = useRef(false);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    // 뒤로 가기로 bfcache의 문서가 복원되면 다시 로그인할 수 있게 합니다.
    const reset = () => {
      locked.current = false;
      setPending(false);
    };
    window.addEventListener('pageshow', reset);
    return () => window.removeEventListener('pageshow', reset);
  }, []);

  const startLogin = () => {
    if (locked.current || onBeforeLogin?.() === false) return;
    locked.current = true;
    setPending(true);
    analytics.track({ name: 'login_started', from: toPathPattern(window.location.pathname) });
  };

  return (
    <>
      <button type="button" className={className} disabled={pending} onClick={startLogin}>
        {children}
      </button>
      {pending && createPortal(<LoginProgress />, document.body)}
    </>
  );
}

function LoginProgress() {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    // top layer에 올려 이미 열린 로그인 모달도 비활성화합니다.
    dialog.showModal();
    dialog.focus();
    const { overflow } = document.body.style;
    document.body.style.overflow = 'hidden';

    // 첫 프레임에서 오버레이가 그려진 뒤 다음 프레임에 문서 이동을 시작합니다.
    let frame = requestAnimationFrame(() => {
      frame = requestAnimationFrame(() => window.location.assign(getGithubLoginUrl()));
    });

    return () => {
      cancelAnimationFrame(frame);
      dialog.close();
      document.body.style.overflow = overflow;
    };
  }, []);

  return (
    <dialog
      ref={dialogRef}
      tabIndex={-1}
      aria-label="GitHub 로그인으로 이동 중"
      aria-busy="true"
      onCancel={(event) => event.preventDefault()}
      onKeyDown={(event) => {
        if (event.key === 'Tab' || event.key === 'Escape') event.preventDefault();
      }}
      className="fixed inset-0 m-0 h-dvh max-h-none w-screen max-w-none bg-transparent p-0 text-white outline-none backdrop:bg-black/60"
    >
      <div className="flex h-full flex-col items-center justify-center gap-4" role="status">
        <span
          aria-hidden="true"
          className="size-10 rounded-full border-4 border-white/30 border-t-white motion-safe:animate-spin"
        />
        <p className="text-sm font-medium">GitHub 로그인으로 이동 중…</p>
      </div>
    </dialog>
  );
}
