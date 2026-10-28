import { useState } from 'react';

import { createFormStore, type FormStore } from '@/utils/form-store';

/**
 * 컴포넌트가 살아 있는 동안 같은 폼 저장소를 돌려준다.
 *
 * 저장소는 첫 렌더에 한 번만 만들고, `key`가 바뀌어 컴포넌트가 새로 만들어지면 저장소도 새로 만든다.
 * 초기값에 `null`로 시작하는 필드가 있으면 추론이 `null`로 좁혀지므로 타입을 직접 지정한다.
 *
 * ```tsx
 * const form = useFormStore<ProjectFormValues>(EMPTY_FORM);
 * ```
 */
export function useFormStore<Values extends object>(initialValues: Values): FormStore<Values> {
  const [store] = useState(() => createFormStore(initialValues));
  return store;
}
