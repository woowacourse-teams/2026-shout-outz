import {
  createContext,
  useContext,
  useEffect,
  useId,
  useRef,
  useState,
  type ReactNode,
} from 'react';

import { Button } from '@/components/Button';
import { cn } from '@/utils/cn';

export interface DropdownProps {
  trigger: ReactNode;
  'aria-label': string;
  children: ReactNode;
  triggerClassName?: string;
  menuClassName?: string;
}

export interface DropdownItemProps {
  children: ReactNode;
  onSelect: () => void;
  disabled?: boolean;
}

const DropdownContext = createContext<(() => void) | null>(null);

function DropdownItem({ children, onSelect, disabled = false }: DropdownItemProps) {
  const close = useContext(DropdownContext);

  return (
    <button
      type="button"
      role="menuitem"
      tabIndex={-1}
      disabled={disabled}
      className="flex w-full items-center rounded-md px-3 py-2 text-left text-sm text-gray-900 hover:bg-gray-100 focus:bg-gray-100 focus:outline-none disabled:cursor-not-allowed disabled:text-gray-500"
      onClick={() => {
        close?.();
        onSelect();
      }}
    >
      {children}
    </button>
  );
}

function DropdownRoot({
  trigger,
  children,
  triggerClassName,
  menuClassName,
  'aria-label': label,
}: DropdownProps) {
  const id = useId();
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  const close = () => {
    setOpen(false);
  };

  useEffect(() => {
    if (!open) return;

    menuRef.current?.focus();

    const onPointerDown = (event: PointerEvent) => {
      if (event.target instanceof Node && !rootRef.current?.contains(event.target)) {
        setOpen(false);
      }
    };

    document.addEventListener('pointerdown', onPointerDown);
    return () => document.removeEventListener('pointerdown', onPointerDown);
  }, [open]);

  return (
    <DropdownContext.Provider value={close}>
      <div
        ref={rootRef}
        className="relative inline-block shrink-0"
        onKeyDown={(event) => {
          if (event.key === 'Escape' && open) {
            event.preventDefault();
            setOpen(false);
            rootRef.current?.querySelector<HTMLButtonElement>('button')?.focus();
          }
          if (!open || !['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return;
          event.preventDefault();
          const items = Array.from(
            menuRef.current?.querySelectorAll<HTMLButtonElement>(
              '[role="menuitem"]:not(:disabled)',
            ) ?? [],
          );
          if (!items.length) return;
          const index = items.indexOf(document.activeElement as HTMLButtonElement);
          const next =
            event.key === 'Home'
              ? 0
              : event.key === 'End'
                ? items.length - 1
                : event.key === 'ArrowDown'
                  ? (index + 1) % items.length
                  : (index <= 0 ? items.length : index) - 1;
          items[next]?.focus();
        }}
        onBlur={(event) => {
          if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false);
        }}
      >
        <Button
          id={id}
          variant="ghost"
          size="sm"
          className={triggerClassName}
          aria-label={label}
          aria-haspopup="menu"
          aria-expanded={open}
          aria-controls={open ? `${id}-menu` : undefined}
          onClick={() => setOpen((current) => !current)}
        >
          {trigger}
        </Button>
        {open && (
          <div
            ref={menuRef}
            id={`${id}-menu`}
            role="menu"
            aria-labelledby={id}
            tabIndex={-1}
            className={cn(
              'bg-background absolute right-0 z-50 mt-2 w-40 rounded-lg border border-gray-200 p-1 shadow-lg outline-none',
              menuClassName,
            )}
          >
            {children}
          </div>
        )}
      </div>
    </DropdownContext.Provider>
  );
}

export const Dropdown = Object.assign(DropdownRoot, { Item: DropdownItem });
