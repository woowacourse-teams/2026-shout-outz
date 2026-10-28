/**
 * 폼 값을 React state 밖에 두는 저장소.
 *
 * 값이 바뀌어도 저장소를 만든 컴포넌트는 다시 렌더되지 않는다. 값을 그리는 컴포넌트만
 * `useFormSelector`·`useFormField`로 필요한 조각을 구독하고, 그 조각이 바뀔 때만 다시 렌더된다.
 * 제출처럼 렌더와 무관하게 값이 필요할 때는 `getValues()`로 읽는다.
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
      // 객체를 새로 만들되 바뀌지 않은 필드는 같은 참조로 남겨, 그 필드의 구독자는 다시 렌더되지 않게 한다.
      values = { ...values, [name]: value };
      listeners.forEach((listener) => listener());
    },
    subscribe: (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
  };
}
