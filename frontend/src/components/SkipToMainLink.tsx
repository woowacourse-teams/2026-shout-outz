import type { MouseEvent } from 'react';

/** 페이지마다 `<main>`에 id를 두지 않고, 현재 문서의 `<main>`을 찾아 포커스를 옮깁니다. */
const focusMain = (event: MouseEvent<HTMLAnchorElement>) => {
  const main = document.querySelector('main');
  if (!main) return;

  event.preventDefault();
  // 원래 포커스를 받을 수 없는 요소라, 옮기는 동안만 코드로 포커스할 수 있게 하고 벗어나면 되돌립니다.
  main.setAttribute('tabindex', '-1');
  main.addEventListener('blur', () => main.removeAttribute('tabindex'), { once: true });

  // 기본 스크롤은 본문 시작을 화면 맨 위에 맞춰 sticky 헤더 아래로 가리므로, 헤더 높이만큼 띄웁니다.
  main.focus({ preventScroll: true });
  const headerHeight = event.currentTarget.closest('header')?.offsetHeight ?? 0;
  window.scrollTo({
    top: Math.max(0, main.getBoundingClientRect().top + window.scrollY - headerHeight),
  });
};

/** 키보드로 처음 Tab을 누르면 나타나, 헤더를 건너뛰고 본문으로 이동합니다. */
export function SkipToMainLink() {
  return (
    <a
      href="#main"
      onClick={focusMain}
      // not-sr-only는 padding도 0으로 되돌리므로, 나타날 때의 여백은 focus:에서 다시 지정합니다.
      className="bg-background focus-visible:outline-primary-600 sr-only rounded-md text-sm font-semibold text-gray-900 shadow-lg focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-60 focus:px-4 focus:py-2.5 focus-visible:outline-2"
    >
      본문으로 바로가기
    </a>
  );
}
