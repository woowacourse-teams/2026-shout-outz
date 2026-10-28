import { useEffect, useRef, useSyncExternalStore } from 'react';

export const TITLE_ANNOUNCER_ID = 'page-title-announcer';

const subscribe = () => () => {};
const getClientSnapshot = () => false;
const getServerSnapshot = () => true;

/** 문서 제목을 출력하고, 마운트 또는 제목 변경 시 스크린리더에 안내합니다. */
export function Title({ children }: { children: string }) {
  const isHydrating = useSyncExternalStore(subscribe, getClientSnapshot, getServerSnapshot);
  // 서버 HTML의 제목은 이미 안내되므로 최초 하이드레이션 제목을 기준값으로 둡니다.
  const lastAnnouncedTitle = useRef(isHydrating ? children : null);

  useEffect(() => {
    if (isHydrating || lastAnnouncedTitle.current === children) return;

    const announcement = document.getElementById(TITLE_ANNOUNCER_ID);
    if (!announcement) return;

    // 같은 제목의 다른 페이지로 이동해도 변경으로 전달합니다.
    announcement.textContent = '';
    const frame = requestAnimationFrame(() => {
      announcement.textContent = children;
      lastAnnouncedTitle.current = children;
    });
    return () => cancelAnimationFrame(frame);
  }, [isHydrating, children]);

  return <title>{children}</title>;
}
