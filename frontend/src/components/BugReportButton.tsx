import { IconBug } from '@tabler/icons-react';
import { useRouterState } from '@tanstack/react-router';

import { Button } from '@/components/Button';
import { BugReportModal } from '@/components/modals/BugReportModal';
import { useModal } from '@/hooks/useModal';

/**
 * 화면 우측 하단에 떠 있는 버그 제보 말풍선.
 *
 * 모양은 `ShareButton`의 말풍선(파란 배경, 오른쪽 아래 꼬리)을 따른다. 모바일에서는 화면을 덜 가리도록
 * 아이콘만 남긴 동그란 버튼이 된다.
 */
export function BugReportButton() {
  const { open } = useModal();
  const pathname = useRouterState({ select: (state) => state.location.pathname });

  const openReport = () => {
    void open<void>((close) => <BugReportModal pagePath={pathname} onClose={() => close()} />);
  };

  return (
    <Button
      variant="outline"
      aria-label="버그 제보하기"
      onClick={openReport}
      className="fixed right-4 bottom-4 z-40 h-auto gap-2 rounded-2xl border-blue-200 bg-blue-50 px-4 py-3 text-blue-700 shadow-md after:absolute after:right-6 after:-bottom-2 after:size-4 after:rotate-45 after:border-r after:border-b after:border-blue-200 after:bg-blue-50 after:transition-colors hover:bg-blue-100 hover:after:bg-blue-100 max-sm:size-12 max-sm:rounded-full max-sm:p-0 max-sm:after:hidden md:right-6 md:bottom-6"
    >
      <IconBug className="size-5 shrink-0" aria-hidden="true" />
      <span className="text-sm font-semibold max-sm:hidden">버그 제보</span>
    </Button>
  );
}
