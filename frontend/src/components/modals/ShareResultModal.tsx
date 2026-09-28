import { IconCircleCheckFilled, IconCircleXFilled } from '@tabler/icons-react';

import { Button } from '@/components/Button';
import { Modal } from '@/components/Modal';

export function ShareResultModal({
  success,
  onClose,
}: {
  success: boolean;
  onClose: () => void;
}) {
  return (
    <Modal onClose={onClose} className="md:w-110">
      <div className="flex flex-col items-center gap-3 px-6 py-8 text-center">
        {success ? (
          <IconCircleCheckFilled
            className="size-12 text-primary-500"
            aria-hidden="true"
          />
        ) : (
          <IconCircleXFilled className="size-12 text-red-500" aria-hidden="true" />
        )}
        <h2 className="text-lg font-bold">{success ? '주소가 복사됐어요' : '주소를 복사하지 못했어요'}</h2>
        <p className="text-sm text-gray-600">
          {success
            ? '복사한 링크를 원하는 곳에 붙여넣어 공유해 보세요.'
            : '브라우저에서 클립보드 사용을 허용한 뒤 다시 시도해 주세요.'}
        </p>
        <Button
          onClick={onClose}
          className={
            success
              ? 'mt-2 min-w-24'
              : 'mt-2 min-w-24 bg-red-500 text-white hover:bg-red-600 focus-visible:ring-red-500'
          }
        >
          확인
        </Button>
      </div>
    </Modal>
  );
}
