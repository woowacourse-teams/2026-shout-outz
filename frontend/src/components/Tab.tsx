import {
  createContext,
  useContext,
  type ComponentPropsWithoutRef,
  type MouseEvent,
  type ReactNode,
} from 'react';

export type TabVariant = 'underline' | 'weak' | 'chip';
export type TabSize = 'sm' | 'md' | 'lg';

export type TabProps = Omit<ComponentPropsWithoutRef<'div'>, 'children' | 'onChange'> & {
  variant?: TabVariant;
  size?: TabSize;
  value?: string;
  onChange?: (value: string) => void;
  children: ReactNode;
};

export type TabItemProps = Omit<
  ComponentPropsWithoutRef<'button'>,
  'children' | 'value' | 'type' | 'role' | 'aria-selected'
> & {
  value: string;
  children: ReactNode;
};

type TabContextValue = Pick<TabProps, 'value' | 'onChange'> & {
  variant: TabVariant;
  size: TabSize;
};

const tabListBaseStyle = 'inline-flex items-center';

const tabListStyles: Record<TabVariant, string> = {
  underline: '',
  weak: 'gap-1',
  chip: 'gap-2',
};

const tabItemBaseStyle =
  'inline-flex cursor-pointer appearance-none items-center justify-center whitespace-nowrap font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-500';

const tabItemSizeStyles: Record<TabSize, string> = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2 text-base',
  lg: 'px-5 py-2.5 text-lg',
};

const tabItemStyles: Record<TabVariant, string> = {
  underline:
    'border-b-3 border-transparent bg-transparent text-gray-500 aria-selected:border-gray-900 aria-selected:text-gray-900',
  weak: 'rounded-lg border-0 bg-transparent text-gray-600 aria-selected:bg-primary-50 aria-selected:text-primary-600',
  chip: 'rounded-full border-0 bg-gray-100 text-gray-600 aria-selected:bg-gray-900 aria-selected:text-white',
};

const TabContext = createContext<TabContextValue | null>(null);

function TabRoot({
  variant = 'underline',
  size = 'md',
  value,
  onChange,
  className,
  children,
  ...props
}: TabProps) {
  return (
    <TabContext.Provider value={{ variant, size, value, onChange }}>
      <div
        {...props}
        role="tablist"
        data-variant={variant}
        data-size={size}
        className={[tabListBaseStyle, tabListStyles[variant], className].filter(Boolean).join(' ')}
      >
        {children}
      </div>
    </TabContext.Provider>
  );
}

function TabItem({ value, className, onClick, children, ...props }: TabItemProps) {
  const context = useContext(TabContext);

  if (context === null) {
    throw new Error('Tab.Item은 Tab 안에서 사용해야 합니다.');
  }

  const isSelected = context.value === value;

  const handleClick = (event: MouseEvent<HTMLButtonElement>) => {
    onClick?.(event);

    if (!event.defaultPrevented && !isSelected) {
      context.onChange?.(value);
    }
  };

  return (
    <button
      {...props}
      type="button"
      role="tab"
      aria-selected={isSelected}
      className={[
        tabItemBaseStyle,
        tabItemSizeStyles[context.size],
        tabItemStyles[context.variant],
        className,
      ]
        .filter(Boolean)
        .join(' ')}
      onClick={handleClick}
    >
      {children}
    </button>
  );
}

export const Tab = Object.assign(TabRoot, { Item: TabItem });
