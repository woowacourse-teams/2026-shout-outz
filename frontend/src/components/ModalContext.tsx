import { createContext, type ReactNode } from 'react';

/**
 * 모달을 여는 함수 하나만 노출한다.
 *
 * `render`는 결과를 들고 닫는 `close`를 받는다. 사용자가 배경이나 ESC로 닫으면 결과가 없으므로
 * `undefined`로 끝난다. 호출부는 `undefined`를 "취소"로 읽으면 된다.
 */
export interface ModalContextValue {
  open: <T>(render: (close: (result: T) => void) => ReactNode) => Promise<T | undefined>;
}

export const ModalContext = createContext<ModalContextValue | null>(null);
