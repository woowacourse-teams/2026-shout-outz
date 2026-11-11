import { IconShare } from '@tabler/icons-react';

import { useShareLink } from '@/hooks/useShareLink';
import { cn } from '@/utils/cn';

export function ShareButton({ className }: { className?: string }) {
  const share = useShareLink();

  return (
    <button
      type="button"
      className={cn(
        'text-primary-600 hover:text-primary-700 focus-visible:outline-primary-600 inline-flex cursor-pointer items-center gap-1.5 rounded-sm text-xs font-semibold focus-visible:outline-2 focus-visible:outline-offset-2',
        className,
      )}
      onClick={() => void share()}
    >
      <IconShare className="size-4" aria-hidden="true" />
      친구에게 공유
    </button>
  );
}
