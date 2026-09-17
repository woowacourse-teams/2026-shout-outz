import { useId, type ReactNode } from 'react';

/**
 * 라벨과 입력, 오류 문구를 묶는 폼 필드.
 *
 * 라벨과 입력을 이어줄 id를 만들어 `children`에 넘긴다. 입력이 `<label for>`로 이어지지 않는
 * 컴포넌트(예: `Select`)라면 `${id}-label`을 `aria-labelledby`로 가리키면 된다.
 *
 * ```tsx
 * <Field label="프로젝트 이름 *" error={errors.title}>
 *   {(id) => <Input id={id} value={title} onChange={...} />}
 * </Field>
 * ```
 */
export interface FieldProps {
  label: string;
  error?: string;
  children: (id: string) => ReactNode;
}

export function Field({ label, error, children }: FieldProps) {
  const id = useId();

  return (
    <div className="flex flex-col gap-2">
      <label id={`${id}-label`} htmlFor={id} className="text-sm font-medium text-gray-900">
        {label}
      </label>
      {children(id)}
      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  );
}
