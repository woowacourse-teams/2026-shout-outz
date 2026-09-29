import { IconShare } from '@tabler/icons-react';

import { Button } from '@/components/Button';
import { ShareResultModal } from '@/components/modals/ShareResultModal';
import { useModal } from '@/hooks/useModal';

export function ShareButton({ url, className }: { url: string; className?: string }) {
  const { open } = useModal();

  const showFailure = () => {
    void open<void>((close) => (
      <ShareResultModal onClose={() => close()} />
    ));
  };

  const share = async () => {
    if (!navigator.share) {
      showFailure();
      return;
    }

    try {
      await navigator.share({ title: document.title, url });
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') return;
      showFailure();
    }
  };

  return (
    <Button
      variant="ghost"
      size="sm"
      className={className}
      aria-label="공유"
      onClick={() => void share()}
    >
      <IconShare className="size-4" aria-hidden="true" />
    </Button>
  );
}
