import { getMountMode, hasServerRenderedMarkup, readHydrationState } from './hydration-state';

type HydrationState = NonNullable<Window['$_TSR']>;

const setReadyState = (value: DocumentReadyState) =>
  Object.defineProperty(document, 'readyState', { value, configurable: true });

afterEach(() => {
  delete (document as { readyState?: DocumentReadyState }).readyState;
  delete window.$_TSR;
  document.body.innerHTML = '';
});

describe('readHydrationState', () => {
  it('문서를 읽는 중에 실행되면, 스트림 끝의 $_TSR이 도착할 때까지(DOMContentLoaded) 기다렸다가 읽는다', async () => {
    setReadyState('loading');
    let settled = false;
    const reading = readHydrationState().then((state) => {
      settled = true;
      return state;
    });

    await Promise.resolve();
    expect(settled).toBe(false);

    // 스트림 끝에서 TanStack Router가 $_TSR을 넣고, 이어서 문서 읽기가 끝난다.
    const state = { router: {} } as unknown as HydrationState;
    window.$_TSR = state;
    setReadyState('interactive');
    document.dispatchEvent(new Event('DOMContentLoaded'));

    await expect(reading).resolves.toBe(state);
  });

  it('문서를 다 읽은 뒤 실행되면 기다리지 않고 바로 읽는다', async () => {
    setReadyState('interactive');
    const state = { router: {} } as unknown as HydrationState;
    window.$_TSR = state;

    await expect(readHydrationState()).resolves.toBe(state);
  });

  it('CSR 셸처럼 $_TSR이 없으면 undefined', async () => {
    setReadyState('complete');

    await expect(readHydrationState()).resolves.toBeUndefined();
  });
});

describe('hasServerRenderedMarkup', () => {
  it.each(['<!--$--><main></main><!--/$-->', '<!--$?--><template id="B:0"></template><!--/$-->'])(
    '서버 렌더 HTML의 Suspense 표시(%s)가 있으면 true',
    (html) => {
      document.body.innerHTML = html;
      expect(hasServerRenderedMarkup()).toBe(true);
    },
  );

  it('script만 있는 CSR 셸이면 false', () => {
    document.body.innerHTML = '<script type="module" src="/main.js"></script>';
    expect(hasServerRenderedMarkup()).toBe(false);
  });

  it('브라우저 확장이 넣은 요소나 다른 주석은 서버 렌더로 보지 않는다', () => {
    document.body.innerHTML = '<div id="extension-root"></div><!-- note -->';
    expect(hasServerRenderedMarkup()).toBe(false);
  });
});

describe('getMountMode', () => {
  it('$_TSR이 있으면 하이드레이트한다', () => {
    expect(getMountMode(true, true)).toBe('hydrate');
  });

  it('CSR 셸이면 새로 그린다', () => {
    expect(getMountMode(false, false)).toBe('render');
  });

  it('서버 HTML은 있는데 $_TSR이 없으면 서버 렌더를 잃은 것으로 구분한다', () => {
    expect(getMountMode(false, true)).toBe('render-without-hydration-state');
  });
});
