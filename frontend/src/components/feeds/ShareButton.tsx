import { IconShare } from '@tabler/icons-react';

import { Button } from '@/components/Button';
import { ShareResultModal } from '@/components/modals/ShareResultModal';
import { useModal } from '@/hooks/useModal';

export function ShareButton({ url, className }: { url: string; className?: string }) {
  const { open } = useModal();

  const showResult = (copied: boolean) => {
    void open<void>((close) => <ShareResultModal copied={copied} onClose={() => close()} />);
  };

  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(url);
      showResult(true);
    } catch {
      showResult(false);
    }
  };

  const share = async () => {
    if (!navigator.share) {
      await copyLink();
      return;
    }

    try {
      await navigator.share({ title: document.title, url });
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') return;
      await copyLink();
    }
  };

  return (
    <Button
      variant="outline"
      size="sm"
      className={`relative h-auto min-h-16 gap-3 rounded-2xl border-blue-200 bg-blue-50 px-4 py-3 text-left shadow-sm after:absolute after:right-7 after:-bottom-2 after:size-4 after:rotate-45 after:border-r after:border-b after:border-blue-200 after:bg-blue-50 hover:bg-blue-100 max-sm:min-h-8 max-sm:rounded-full max-sm:p-2 max-sm:after:hidden ${className ?? ''}`}
      aria-label="공유"
      onClick={() => void share()}
    >
      <span className="hidden text-xs leading-5 font-semibold sm:block">
        <span className="block text-gray-700">“이건 내 친구가 딱인데?”</span>
        <span className="text-primary-600">친구에게 공유해보세요!</span>
      </span>
      <IconShare className="text-primary-600 size-5 shrink-0" aria-hidden="true" />
    </Button>
  );
}
