import { IconCircleCheckFilled, IconCircleXFilled } from '@tabler/icons-react';

import { Button } from '@/components/Button';
import { Modal } from '@/components/Modal';

export function ShareResultModal({ onClose, copied }: { onClose: () => void; copied: boolean }) {
  return (
    <Modal onClose={onClose} className="md:w-110">
      <div className="flex flex-col items-center gap-3 px-6 py-8 text-center">
        {copied ? (
          <IconCircleCheckFilled className="size-12 text-green-500" aria-hidden="true" />
        ) : (
          <IconCircleXFilled className="size-12 text-red-500" aria-hidden="true" />
        )}
        <h2 className="text-lg font-bold">
          {copied ? '링크를 복사했어요' : '링크를 복사하지 못했어요'}
        </h2>
        <p className="text-sm text-gray-600">
          {copied
            ? '게시글 링크를 클립보드에 복사했습니다. 원하는 곳에 붙여넣어 공유해 주세요.'
            : '시스템 공유와 클립보드 복사에 실패했습니다. 브라우저 권한을 확인한 뒤 다시 시도해 주세요.'}
        </p>
        <Button onClick={onClose} className="mt-2 min-w-24">
          확인
        </Button>
      </div>
    </Modal>
  );
}
