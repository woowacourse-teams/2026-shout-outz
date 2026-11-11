import { IconShare } from '@tabler/icons-react';

import { ShareResultModal } from '@/components/modals/ShareResultModal';
import { useModal } from '@/hooks/useModal';
import { cn } from '@/utils/cn';

export function ShareButton({ className }: { className?: string }) {
  const { open } = useModal();

  const showResult = (copied: boolean) => {
    void open<void>((close) => <ShareResultModal copied={copied} onClose={() => close()} />);
  };

  const copyLink = async (url: string) => {
    try {
      await navigator.clipboard.writeText(url);
      showResult(true);
    } catch {
      showResult(false);
    }
  };

  const share = async () => {
    const url = window.location.href;

    if (!navigator.share) {
      await copyLink(url);
      return;
    }

    try {
      await navigator.share({ title: document.title, url });
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') return;
      await copyLink(url);
    }
  };

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
