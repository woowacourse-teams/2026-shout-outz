import { IconCircleXFilled } from '@tabler/icons-react';

import { Button } from '@/components/Button';
import { Modal } from '@/components/Modal';

export function ShareResultModal({ onClose }: { onClose: () => void }) {
  return (
    <Modal onClose={onClose} className="md:w-110">
      <div className="flex flex-col items-center gap-3 px-6 py-8 text-center">
        <IconCircleXFilled className="size-12 text-red-500" aria-hidden="true" />
        <h2 className="text-lg font-bold">공유를 시작하지 못했어요</h2>
        <p className="text-sm text-gray-600">
          이 브라우저에서 시스템 공유를 지원하는지 확인한 뒤 다시 시도해 주세요.
        </p>
        <Button
          onClick={onClose}
          className="mt-2 min-w-24 bg-red-500 text-white hover:bg-red-600 focus-visible:ring-red-500"
        >
          확인
        </Button>
      </div>
    </Modal>
  );
}
