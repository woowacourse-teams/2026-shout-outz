import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

/**
 * 조건부 클래스를 결합하고 충돌하는 Tailwind 클래스를 정리한다.
 *
 * 뒤에 오는 클래스가 앞의 같은 그룹 클래스를 덮으므로, 컴포넌트가 만든
 * 기본 클래스를 호출부의 `className`으로 덮어쓸 수 있다.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
