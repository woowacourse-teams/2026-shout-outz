import { useEffect } from 'react';
import { useRouter } from '@tanstack/react-router';

/**
 * 문서의 처음으로 포커스를 되돌립니다. 다음 Tab은 문서의 첫 요소(본문 바로가기)부터 시작합니다.
 * body는 원래 포커스를 받을 수 없어, 옮기는 동안만 코드로 포커스할 수 있게 하고 벗어나면 되돌립니다.
 */
const resetFocus = () => {
  // 이동 중에도 열려 있는 모달 안의 포커스는 건드리지 않습니다.
  if (document.activeElement?.closest('dialog[open]')) return;

  const { body } = document;
  body.setAttribute('tabindex', '-1');
  body.addEventListener('blur', () => body.removeAttribute('tabindex'), { once: true });
  // 스크롤 위치는 라우터의 스크롤 복원이 정하므로 건드리지 않습니다.
  body.focus({ preventScroll: true });
};

/**
 * 다른 페이지로 이동하면 포커스를 문서의 처음으로 되돌립니다.
 * 첫 접속과, 경로는 그대로 두고 검색 조건(쿼리)이나 해시만 바뀌는 같은 페이지 안의 변화는 제외합니다.
 */
export function RouteFocusReset() {
  const router = useRouter();

  useEffect(
    () =>
      router.subscribe('onRendered', ({ fromLocation, pathChanged }) => {
        if (!fromLocation || !pathChanged) return;
        resetFocus();
      }),
    [router],
  );

  return null;
}
