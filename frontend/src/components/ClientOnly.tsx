import { useSyncExternalStore, type ReactNode } from 'react';

const subscribe = () => () => {};
const getClientSnapshot = () => true;
const getServerSnapshot = () => false;

/** 서버 렌더·정적 HTML과 첫 hydration에서는 false, 그 뒤 브라우저 렌더에서는 true입니다. */
export function useIsClient() {
  return useSyncExternalStore(subscribe, getClientSnapshot, getServerSnapshot);
}

/** 정적 HTML과 첫 hydration에서는 비우고, 브라우저에서만 사용자별 기능을 렌더링합니다. */
export function ClientOnly({ children }: { children: ReactNode }) {
  return useIsClient() ? children : null;
}
