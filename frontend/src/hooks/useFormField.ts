import { useCallback } from 'react';

import { useFormSelector } from '@/hooks/useFormSelector';
import type { FormStore } from '@/utils/form-store';

/**
 * 필드 하나의 값과 바꾸는 함수. 이 필드가 바뀔 때만 부른 컴포넌트가 다시 렌더된다.
 *
 * ```tsx
 * const [title, setTitle] = useFormField(form, 'title');
 * <input value={title} onChange={(event) => setTitle(event.target.value)} />
 * ```
 */
export function useFormField<Values extends object, Key extends keyof Values>(
  store: FormStore<Values>,
  name: Key,
) {
  const value = useFormSelector(store, (values) => values[name]);
  // useState의 setter처럼 렌더가 바뀌어도 같은 함수를 돌려준다.
  const setValue = useCallback((next: Values[Key]) => store.setValue(name, next), [store, name]);
  return [value, setValue] as const;
}
