import { useCallback, useState, type ReactNode } from 'react';

import { ModalContext } from '@/components/ModalContext';

interface ModalEntry {
  id: string;
  render: (close: (result: unknown) => void) => ReactNode;
  resolve: (result: unknown) => void;
}

export function ModalProvider({ children }: { children: ReactNode }) {
  const [stack, setStack] = useState<ModalEntry[]>([]);

  const close = useCallback((id: string, result: unknown) => {
    setStack((previous) => {
      const target = previous.find((entry) => entry.id === id);
      if (!target) return previous;

      target.resolve(result);
      return previous.filter((entry) => entry.id !== id);
    });
  }, []);

  const open = useCallback(
    <T,>(render: (close: (result: T) => void) => ReactNode) =>
      new Promise<T | undefined>((resolve) => {
        setStack((previous) => [
          ...previous,
          {
            id: crypto.randomUUID(),
            render: render as ModalEntry['render'],
            resolve: resolve as ModalEntry['resolve'],
          },
        ]);
      }),
    [],
  );

  return (
    <ModalContext.Provider value={{ open }}>
      {children}
      {stack.map((entry) => (
        <ModalRoot key={entry.id} entry={entry} onClose={close} />
      ))}
    </ModalContext.Provider>
  );
}

function ModalRoot({
  entry,
  onClose,
}: {
  entry: ModalEntry;
  onClose: (id: string, result: unknown) => void;
}) {
  return <>{entry.render((result) => onClose(entry.id, result))}</>;
}
