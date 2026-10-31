import type { ComponentProps, ReactNode } from 'react';

import { Input, type InputProps } from '@/components/Input';
import { useFormField } from '@/hooks/useFormField';
import { useFormSelector } from '@/hooks/useFormSelector';
import type { FormStore } from '@/utils/form-store';

/**
 * 폼 저장소(`useFormStore`)에 연결하는 컴포넌트 모음.
 *
 * 구독은 이 컴포넌트들 안에서 일어나므로, 값이 바뀌면 이 컴포넌트와 children 함수가 그린 부분만
 * 다시 렌더되고 폼을 그리는 상위 컴포넌트는 그대로 남는다.
 */

/** 값이 `Type`인 필드 이름만 고른다. */
type FieldNameOf<Values, Type> = {
  [Key in keyof Values]-?: Values[Key] extends Type ? Key : never;
}[keyof Values];

export interface FormInputProps<Values extends object> extends Omit<
  InputProps,
  'form' | 'name' | 'value' | 'onChange'
> {
  form: FormStore<Values>;
  name: FieldNameOf<Values, string>;
}

/** 문자열 필드 하나에 연결된 `Input`. */
export function FormInput<Values extends object>({ form, name, ...props }: FormInputProps<Values>) {
  const [value, setValue] = useFormField(form, name);
  return (
    <Input
      {...props}
      value={value as string}
      onChange={(event) => setValue(event.target.value as Values[typeof name])}
    />
  );
}

export interface FormTextareaProps<Values extends object> extends Omit<
  ComponentProps<'textarea'>,
  'form' | 'name' | 'value' | 'onChange'
> {
  form: FormStore<Values>;
  name: FieldNameOf<Values, string>;
}

/** 문자열 필드 하나에 연결된 `<textarea>`. */
export function FormTextarea<Values extends object>({
  form,
  name,
  ...props
}: FormTextareaProps<Values>) {
  const [value, setValue] = useFormField(form, name);
  return (
    <textarea
      {...props}
      value={value as string}
      onChange={(event) => setValue(event.target.value as Values[typeof name])}
    />
  );
}

export interface FormControllerProps<Values extends object, Key extends keyof Values> {
  form: FormStore<Values>;
  name: Key;
  children: (field: { value: Values[Key]; onChange: (value: Values[Key]) => void }) => ReactNode;
}

/**
 * 필드 하나를 구독해 값과 바꾸는 함수를 children 함수에 넘긴다.
 * `Select`, `<input>`, 체크박스처럼 값을 받는 prop 이름이 다른 입력을 모두 같은 방식으로 연결한다.
 *
 * ```tsx
 * <FormController form={form} name="categoryId">
 *   {({ value, onChange }) => <Select value={value} onValueChange={onChange} />}
 * </FormController>
 * ```
 */
export function FormController<Values extends object, Key extends keyof Values>({
  form,
  name,
  children,
}: FormControllerProps<Values, Key>) {
  const [value, setValue] = useFormField(form, name);
  return children({ value, onChange: setValue });
}

export interface FormValueProps<Values extends object, Selected> {
  form: FormStore<Values>;
  /** 원시값이나 저장소 안의 값을 그대로 돌려줘야 한다. 이유는 `useFormSelector`를 본다. */
  select: (values: Values) => Selected;
  children: (selected: Selected) => ReactNode;
}

/**
 * 폼 값에서 고른 조각을 구독해 children 함수에 넘긴다. 값을 바꾸지 않고 읽기만 하는 곳에 쓴다.
 * 등록 버튼의 비활성 여부처럼 여러 필드에서 계산한 값이면, 계산 결과가 바뀔 때만 다시 렌더된다.
 *
 * ```tsx
 * <FormValue form={form} select={(values) => !values.title.trim()}>
 *   {(empty) => <Button type="submit" disabled={empty}>등록</Button>}
 * </FormValue>
 * ```
 */
export function FormValue<Values extends object, Selected>({
  form,
  select,
  children,
}: FormValueProps<Values, Selected>) {
  return children(useFormSelector(form, select));
}
