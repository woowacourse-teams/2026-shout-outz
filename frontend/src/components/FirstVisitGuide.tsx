import { useEffect, useState } from 'react';
import { IconArrowUpRight, IconX } from '@tabler/icons-react';
import { Button } from '@/components/Button';
import { Modal } from '@/components/Modal';

const STORAGE_KEY = 'shoutoutz:about-guide-seen';

function hasSeenGuide() {
  try {
    return localStorage.getItem(STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}

/** ClientOnly 안에서 경로별로 마운트합니다. 저장소를 읽는 동안 SSG HTML은 바뀌지 않습니다. */
export function FirstVisitGuide({
  mode,
  onExplore,
}: {
  mode: 'offer' | 'visited' | 'skip';
  onExplore: () => void;
}) {
  const [dismissed, setDismissed] = useState(hasSeenGuide);

  useEffect(() => {
    if (mode === 'skip' || (mode === 'offer' && dismissed)) return;
    try {
      // 안내를 이미 본 브라우저에는 다시 띄우지 않고, 소개 페이지 직접 방문도 포함합니다.
      localStorage.setItem(STORAGE_KEY, 'true');
    } catch {
      // 저장소가 차단돼도 닫기와 페이지 이동은 사용할 수 있습니다.
    }
  }, [mode, dismissed]);

  if (dismissed || mode !== 'offer') return null;
  const close = () => setDismissed(true);

  return (
    <Modal
      onClose={close}
      labelledBy="first-visit-title"
      describedBy="first-visit-description"
      className="relative md:w-110"
    >
      <Button
        variant="ghost"
        size="sm"
        aria-label="소개 안내 닫기"
        onClick={close}
        className="absolute top-4 right-4 size-8 rounded-full p-0 text-gray-500"
      >
        <IconX className="size-4" aria-hidden="true" />
      </Button>
      <div className="overflow-y-auto px-7 pt-10 pb-7 text-center md:px-10 md:pt-10 md:pb-8">
        <img
          src="/about-assets/character-small.webp"
          alt=""
          width={104}
          height={104}
          className="mx-auto mb-5 size-26 object-contain"
        />
        <p className="text-primary-600 mb-2 text-xs font-semibold">처음 오셨나요?</p>
        <h2 id="first-visit-title" className="text-xl leading-7 font-bold break-keep">
          샤라웃을 소개할게요
        </h2>
        <p id="first-visit-description" className="mt-3 text-sm leading-6 break-keep text-gray-600">
          프로젝트를 둘러보고, 개발과 취업 고민을 나누는 곳이에요. 샤라웃에서 무엇을 할 수 있는지
          살펴보세요.
        </p>
        <Button
          size="lg"
          className="mt-6 w-full gap-2"
          data-modal-autofocus
          onClick={() => {
            close();
            onExplore();
          }}
        >
          샤라웃 알아보기 <IconArrowUpRight className="size-4" aria-hidden="true" />
        </Button>
        <Button variant="ghost" size="sm" className="mt-2 text-gray-500" onClick={close}>
          바로 둘러볼게요
        </Button>
      </div>
    </Modal>
  );
}
