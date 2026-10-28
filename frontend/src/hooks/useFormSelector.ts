import { useSyncExternalStore } from 'react';

import type { FormStore } from '@/utils/form-store';

/**
 * 폼 값에서 `selector`가 고른 조각만 구독한다. 저장소가 바뀌어도 조각이 `Object.is`로 같으면
 * 다시 렌더되지 않는다.
 *
 * `selector`는 원시값이나 저장소 안의 값을 그대로 돌려줘야 한다. 부를 때마다 새 객체나 배열을
 * 만들면 매번 바뀐 값으로 보여 렌더가 끝나지 않는다.
 */
export function useFormSelector<Values extends object, Selected>(
  store: FormStore<Values>,
  selector: (values: Values) => Selected,
): Selected {
  const getSnapshot = () => selector(store.getValues());
  return useSyncExternalStore(store.subscribe, getSnapshot, getSnapshot);
}
