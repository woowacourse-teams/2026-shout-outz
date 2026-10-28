import type { ComponentProps, ReactNode } from 'react';

import { Input, type InputProps } from '@/components/Input';
import { useFormField } from '@/hooks/useFormField';
import { useFormSelector } from '@/hooks/useFormSelector';
import type { FormStore } from '@/utils/form-store';

/**
 * 폼 저장소(`useFormStore`)에 연결된 컴포넌트 모음.
 *
 * 각 컴포넌트가 필요한 값만 구독하므로, 입력하면 그 값을 쓰는 컴포넌트만 다시 렌더되고
 * 폼을 그리는 상위 컴포넌트와 다른 필드는 그대로 남는다.
 *
 * ```tsx
 * const form = useFormStore(() => ({ title: '', tags: [] }));
 *
 * <FormInput form={form} name="title" />
 * <FormField form={form} name="tags">
 *   {({ value, onChange }) => <TagPicker value={value} onChange={onChange} />}
 * </FormField>
 * <FormValue form={form} select={(values) => !values.title.trim()}>
 *   {(empty) => <Button type="submit" disabled={empty}>등록</Button>}
 * </FormValue>
 * ```
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

export interface FormFieldProps<Values extends object, Key extends keyof Values> {
  form: FormStore<Values>;
  name: Key;
  children: (field: { value: Values[Key]; onChange: (value: Values[Key]) => void }) => ReactNode;
}

/** `Select`나 직접 만든 입력처럼 `value`·`onChange` 모양이 다른 입력을 필드 하나에 연결한다. */
export function FormField<Values extends object, Key extends keyof Values>({
  form,
  name,
  children,
}: FormFieldProps<Values, Key>) {
  const [value, onChange] = useFormField(form, name);
  return children({ value, onChange });
}

export interface FormValueProps<Values extends object, Selected> {
  form: FormStore<Values>;
  /** 원시값이나 저장소 안의 값을 그대로 돌려줘야 한다. 자세한 이유는 `useFormSelector`를 본다. */
  select: (values: Values) => Selected;
  children: (selected: Selected) => ReactNode;
}

/** 폼 값에서 고른 조각이 바뀔 때만 다시 그린다. 등록 버튼의 비활성 여부처럼 값에서 계산한 UI에 쓴다. */
export function FormValue<Values extends object, Selected>({
  form,
  select,
  children,
}: FormValueProps<Values, Selected>) {
  return children(useFormSelector(form, select));
}
