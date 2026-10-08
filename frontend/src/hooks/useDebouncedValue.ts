import { useEffect, useState } from 'react';

/** 값이 `delay`ms 동안 바뀌지 않으면 그 값을 돌려준다. 입력할 때마다 요청하지 않으려고 쓴다. */
export function useDebouncedValue<T>(value: T, delay: number) {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debounced;
}
