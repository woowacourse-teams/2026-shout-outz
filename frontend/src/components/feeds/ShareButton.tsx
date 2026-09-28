import { IconShare } from '@tabler/icons-react';

import { Button } from '@/components/Button';
import { ShareResultModal } from '@/components/modals/ShareResultModal';
import { useModal } from '@/hooks/useModal';

export function ShareButton({ url, className }: { url: string; className?: string }) {
  const { open } = useModal();

  const showResult = (success: boolean) => {
    void open<void>((close) => (
      <ShareResultModal success={success} onClose={() => close()} />
    ));
  };

  const copyLink = async () => {
    if (!navigator.clipboard?.writeText) {
      showResult(false);
      return;
    }

    try {
      await navigator.clipboard.writeText(url);
      showResult(true);
    } catch {
      showResult(false);
    }
  };

  return (
    <Button
      variant="ghost"
      size="sm"
      className={className}
      aria-label="공유"
      onClick={() => void copyLink()}
    >
      <IconShare className="size-4" aria-hidden="true" />
    </Button>
  );
}
