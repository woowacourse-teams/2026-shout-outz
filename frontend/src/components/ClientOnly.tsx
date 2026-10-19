import { useSyncExternalStore, type ReactNode } from 'react';

const subscribe = () => () => {};
const getClientSnapshot = () => true;
const getServerSnapshot = () => false;

/** 정적 HTML과 첫 hydration에서는 비우고, 브라우저에서만 사용자별 기능을 렌더링합니다. */
export function ClientOnly({ children }: { children: ReactNode }) {
  const ready = useSyncExternalStore(subscribe, getClientSnapshot, getServerSnapshot);
  return ready ? children : null;
}
