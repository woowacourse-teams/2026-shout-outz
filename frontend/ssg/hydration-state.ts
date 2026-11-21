/** 브라우저가 이 문서를 어떻게 마운트할지 */
export type MountMode =
  /** 서버가 그린 HTML과 $_TSR이 모두 있다. 그대로 하이드레이트한다. */
  | 'hydrate'
  /** 서버가 그린 HTML이 없는 CSR 셸이다. 새로 그린다. */
  | 'render'
  /** 서버가 그린 HTML은 있는데 $_TSR이 없다. 하이드레이트할 수 없어 서버 HTML을 버리고 새로 그린다. */
  | 'render-without-hydration-state';

// $_TSR은 TanStack Router가 스트림 맨 끝(</body> 직전)에 스크립트 하나로 넣는다. main이 문서를 다 읽기 전에
// 실행되면(<script async>, React bootstrapModules·preinit 등) 아직 $_TSR이 없어 하이드레이션 없이 새로 그리게 된다.
// 이때는 에러가 나지 않고 화면도 정상이라 서버 렌더가 사라져도 알 수 없다. 그래서 스크립트를 어디에 어떻게 넣든
// 문서를 끝까지 읽은 뒤에 $_TSR을 읽는다.
export async function readHydrationState(doc: Document = document): Promise<Window['$_TSR']> {
  if (doc.readyState === 'loading') {
    await new Promise<void>((resolve) => {
      doc.addEventListener('DOMContentLoaded', () => resolve(), { once: true });
    });
  }
  return doc.defaultView?.$_TSR;
}

// 서버 렌더·SSG HTML에는 React가 Suspense 경계마다 <!--$-->, <!--$?-->, <!--$!--> 주석을 남긴다.
// CSR 셸(dist/index.html)의 body에는 script만 있다. 브라우저 확장이 넣는 요소와 헷갈리지 않게 주석으로 판별한다.
const SUSPENSE_MARKER = /^\$[?!]?$/;

export function hasServerRenderedMarkup(doc: Document = document): boolean {
  const walker = doc.createTreeWalker(doc.body, NodeFilter.SHOW_COMMENT);
  for (let node = walker.nextNode(); node; node = walker.nextNode()) {
    if (SUSPENSE_MARKER.test(node.nodeValue ?? '')) return true;
  }
  return false;
}

export function getMountMode(hasHydrationState: boolean, hasServerMarkup: boolean): MountMode {
  if (hasHydrationState) return 'hydrate';
  return hasServerMarkup ? 'render-without-hydration-state' : 'render';
}
