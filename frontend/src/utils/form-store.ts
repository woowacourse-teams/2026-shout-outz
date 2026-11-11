/**
 * 폼 값을 React state 밖에 두는 저장소.
 *
 * 값이 바뀌어도 React는 알지 못하므로, 값을 바꾼 뒤 `subscribe`로 등록된 함수들을 직접 불러 알린다.
 * 컴포넌트는 `useSyncExternalStore`에 `subscribe`와 `getValues`를 넘겨 이 알림을 받는다.
 */
export interface FormStore<Values extends object> {
  getValues: () => Values;
  setValue: <Key extends keyof Values>(name: Key, value: Values[Key]) => void;
  /** 값이 바뀔 때 불릴 함수를 등록하고, 등록을 푸는 함수를 돌려준다. */
  subscribe: (listener: () => void) => () => void;
}

export function createFormStore<Values extends object>(initialValues: Values): FormStore<Values> {
  let values = initialValues;
  const listeners = new Set<() => void>();

  return {
    getValues: () => values,
    setValue: (name, value) => {
      if (Object.is(values[name], value)) return;
      // 객체를 새로 만들되 바뀌지 않은 필드는 같은 참조로 남겨, 그 필드를 보는 컴포넌트는 다시 렌더되지 않게 한다.
      values = { ...values, [name]: value };
      listeners.forEach((listener) => listener());
    },
    subscribe: (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
  };
}
