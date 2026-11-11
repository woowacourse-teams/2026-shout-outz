import { ShareResultModal } from '@/components/modals/ShareResultModal';
import { useModal } from '@/hooks/useModal';

/** 주소를 공유 시트로 공유하고, 공유 시트가 없거나 실패하면 링크를 복사한다. */
export function useShareLink() {
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

  return async ({ url = window.location.href, title = document.title } = {}) => {
    if (!navigator.share) {
      await copyLink(url);
      return;
    }

    try {
      await navigator.share({ title, url });
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') return;
      await copyLink(url);
    }
  };
}
