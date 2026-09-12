import {
  Children,
  createContext,
  isValidElement,
  useContext,
  useId,
  useRef,
  useState,
  type AriaAttributes,
  type ComponentPropsWithoutRef,
  type ReactNode,
  type Ref,
} from 'react';

import { cn } from '@/utils/cn';

type SelectCommonProps = AriaAttributes &
  Pick<ComponentPropsWithoutRef<'button'>, 'id' | 'className'> &
  Pick<ComponentPropsWithoutRef<'select'>, 'name' | 'disabled'>;

export interface SelectProps extends SelectCommonProps {
  children: ReactNode;
  placeholder?: ReactNode;
  value?: string | null;
  defaultValue?: string | null;
  onValueChange?: (value: string) => void;
  ref?: Ref<HTMLButtonElement>;
}

export interface SelectItemProps {
  value: string;
  children: ReactNode;
  disabled?: boolean;
}

export interface SelectGroupProps {
  label: ReactNode;
  children: ReactNode;
}

const SelectContext = createContext<{
  value: string | null;
  select: (value: string) => void;
} | null>(null);

function SelectItem({ value, children, disabled = false }: SelectItemProps) {
  const context = useContext(SelectContext);
  if (!context) return null;

  return (
    <div
      role="option"
      aria-selected={context.value === value}
      aria-disabled={disabled || undefined}
      className={cn(
        'flex min-h-10 cursor-default items-center justify-between gap-3 rounded-md px-3 py-2 text-sm text-gray-900',
        disabled && 'text-gray-500',
      )}
      onMouseDown={(event) => event.preventDefault()}
      onClick={() => !disabled && context.select(value)}
    >
      <span className="min-w-0 flex-1">{children}</span>
      {context.value === value && (
        <svg aria-hidden="true" viewBox="0 0 20 20" fill="none" className="size-4 shrink-0">
          <path
            d="m4.5 10 3.5 3.5 7.5-7.5"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      )}
    </div>
  );
}

function SelectGroup({ label, children }: SelectGroupProps) {
  const labelId = useId();

  return (
    <div role="group" aria-labelledby={labelId}>
      <div id={labelId} className="px-3 py-2 text-xs font-medium text-gray-500">
        {label}
      </div>
      {children}
    </div>
  );
}

function findSelectedItem(children: ReactNode, value: string | null): SelectItemProps | undefined {
  let result: SelectItemProps | undefined;

  Children.forEach(children, (child) => {
    if (!isValidElement(child)) return;

    if (child.type === SelectItem && (child.props as SelectItemProps).value === value) {
      result = child.props as SelectItemProps;
    }

    if (child.type === SelectGroup) {
      const item = findSelectedItem((child.props as SelectGroupProps).children, value);
      if (item) result = item;
    }
  });

  return result;
}

function SelectRoot({
  children,
  placeholder,
  value,
  defaultValue,
  onValueChange,
  ref,
  id,
  className,
  name,
  disabled = false,
  ...ariaProps
}: SelectProps) {
  const generatedId = useId();
  const listboxId = `${id ?? generatedId}-listbox`;
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const controlled = value !== undefined;
  const [internalValue, setInternalValue] = useState<string | null>(defaultValue ?? null);
  const [open, setOpen] = useState(false);
  const isOpen = open && !disabled;
  const selectedValue = controlled ? value : internalValue;
  const selectedItem = findSelectedItem(children, selectedValue);

  const select = (nextValue: string) => {
    if (disabled) return;
    if (nextValue !== selectedValue) {
      if (!controlled) setInternalValue(nextValue);
      onValueChange?.(nextValue);
    }
    setOpen(false);
    triggerRef.current?.focus();
  };

  return (
    <SelectContext.Provider value={{ value: selectedValue, select }}>
      <span className="relative inline-block w-full">
        <button
          {...ariaProps}
          ref={(node) => {
            triggerRef.current = node;
            if (typeof ref === 'function') ref(node);
            else if (ref) ref.current = node;
          }}
          id={id}
          type="button"
          role="combobox"
          aria-controls={isOpen ? listboxId : undefined}
          aria-expanded={isOpen}
          aria-haspopup="listbox"
          className={cn(
            'flex h-11 w-full items-center justify-between gap-2 rounded-lg bg-gray-100 px-4 text-left text-sm text-gray-900 outline-none',
            'focus-visible:ring-primary-600 focus-visible:ring-2',
            'aria-invalid:ring-1 aria-invalid:ring-red-600',
            'disabled:cursor-not-allowed disabled:text-gray-500',
            className,
          )}
          disabled={disabled}
          onBlur={() => setOpen(false)}
          onClick={() => setOpen((current) => !current)}
        >
          <span className={cn('min-w-0 flex-1 truncate', !selectedItem && 'text-gray-500')}>
            {selectedItem ? selectedItem.children : placeholder}
          </span>
          <svg aria-hidden="true" viewBox="0 0 20 20" fill="none" className="size-4 shrink-0">
            <path
              d="m5 7.5 5 5 5-5"
              stroke="currentColor"
              strokeWidth="1.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>

        {isOpen && (
          <div
            id={listboxId}
            role="listbox"
            aria-labelledby={ariaProps['aria-labelledby']}
            aria-label={ariaProps['aria-label']}
            className="bg-background absolute z-50 mt-2 max-h-60 w-full min-w-max overflow-y-auto rounded-lg border border-gray-200 p-1 shadow-lg"
          >
            {children}
          </div>
        )}

        {name && (
          <input type="hidden" name={name} value={selectedValue ?? ''} disabled={disabled} />
        )}
      </span>
    </SelectContext.Provider>
  );
}

export const Select = Object.assign(SelectRoot, {
  Item: SelectItem,
  Group: SelectGroup,
});
