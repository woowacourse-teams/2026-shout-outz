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

export interface DropdownProps {
  trigger: ReactNode;
  'aria-label': string;
  children: ReactNode;
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
      className="flex w-full items-center rounded-md px-3 py-2 text-left text-sm text-gray-900 hover:bg-gray-100 focus:outline-none disabled:cursor-not-allowed disabled:text-gray-500"
      onClick={() => {
        close?.();
        onSelect();
      }}
    >
      {children}
    </button>
  );
}

function DropdownRoot({ trigger, children, 'aria-label': label }: DropdownProps) {
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
        onBlur={(event) => {
          if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false);
        }}
      >
        <Button
          id={id}
          variant="ghost"
          size="sm"
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
            className="bg-background absolute right-0 z-50 mt-2 w-40 rounded-lg border border-gray-200 p-1 shadow-lg outline-none"
          >
            {children}
          </div>
        )}
      </div>
    </DropdownContext.Provider>
  );
}

export const Dropdown = Object.assign(DropdownRoot, { Item: DropdownItem });
