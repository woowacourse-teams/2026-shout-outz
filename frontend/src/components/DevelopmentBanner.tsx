import { useSyncExternalStore } from 'react';

const subscribe = () => () => {};
const getSnapshot = () => {
  const hostname = window.location.hostname;
  return hostname === 'localhost' || hostname.startsWith('staging.');
};
const getServerSnapshot = () => false;

export function DevelopmentBanner() {
  const isStaging = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

  if (!isStaging) return null;

  return (
    <aside
      aria-label="개발 환경 안내"
      className="border-static-yellow-300 bg-static-yellow-100 text-static-yellow-950 border-b px-4 py-3 text-center text-sm leading-6 md:text-base"
    >
      <a
        href="https://shout-ou.tz"
        className="rounded-sm font-semibold underline underline-offset-2 focus-visible:outline-2"
      >
        shout-ou.tz
      </a>
      를 이용해 주세요.
    </aside>
  );
}
