import { useState } from 'react';

import { createFormStore, type FormStore } from '@/utils/form-store';

/** 컴포넌트가 마운트되어 있는 동안 같은 폼 저장소를 돌려준다. 초기값은 처음 한 번만 계산한다. */
export function useFormStore<Values extends object>(
  getInitialValues: () => Values,
): FormStore<Values> {
  const [store] = useState(() => createFormStore(getInitialValues()));
  return store;
}
