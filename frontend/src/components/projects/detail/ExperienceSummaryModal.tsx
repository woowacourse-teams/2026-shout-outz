import { IconX } from '@tabler/icons-react';
import { overlay } from 'overlay-kit';
import type { ComponentType, ReactNode } from 'react';

import type { ProjectExperience } from '@/apis/project-explore';
import { SourceLink } from '@/components/projects/detail/ExperienceSource';
import { Dialog, DialogClose, DialogContent, DialogTitle } from '@/components/ui/dialog';
import { Drawer, DrawerClose, DrawerContent, DrawerTitle } from '@/components/ui/drawer';
import { useMediaQuery } from '@/hooks/useMediaQuery';

/** 경험 요약을 자료 요약과 같이 데스크톱은 모달, 모바일은 바텀시트로 연다. */
export function openExperienceSummary(experience: ProjectExperience) {
  overlay.open(({ isOpen, close, unmount }) => (
    <ExperienceSummaryModal
      experience={experience}
      isOpen={isOpen}
      onClose={close}
      onExit={unmount}
    />
  ));
}

interface ExperienceSummaryModalProps {
  experience: ProjectExperience;
  isOpen: boolean;
  onClose: () => void;
  /** 닫힌 뒤 DOM에서 지울 때 */
  onExit: () => void;
}

function ExperienceSummaryModal({
  experience,
  isOpen,
  onClose,
  onExit,
}: ExperienceSummaryModalProps) {
  const desktop = useMediaQuery('(min-width: 768px)');

  if (desktop) {
    return (
      <Dialog
        open={isOpen}
        onOpenChange={(open) => {
          if (open) return;
          onClose();
          // 모달은 닫히는 애니메이션이 없어 바로 지운다.
          onExit();
        }}
      >
        <DialogContent
          aria-describedby={undefined}
          // DESIGN-TOKEN: 자료 요약 모달과 같게 화면 위아래로 56px씩 띄운 높이 안에서 스크롤한다.
          className="top-14 flex max-h-[calc(100dvh-7rem)] translate-y-0 flex-col overflow-hidden rounded-2xl shadow-2xl sm:max-w-150"
        >
          <div className="overflow-y-auto px-7 pt-7 pb-12">
            <ExperienceSummary
              experience={experience}
              Title={DialogTitle}
              closeButton={(children) => <DialogClose asChild>{children}</DialogClose>}
            />
          </div>
        </DialogContent>
      </Dialog>
    );
  }

  return (
    <Drawer
      open={isOpen}
      onOpenChange={(open) => {
        if (!open) onClose();
      }}
      onAnimationEnd={(open) => {
        if (!open) onExit();
      }}
    >
      <DrawerContent aria-describedby={undefined}>
        <div className="overflow-y-auto px-5 pt-3.5 pb-10">
          <ExperienceSummary
            experience={experience}
            Title={DrawerTitle}
            closeButton={(children) => <DrawerClose asChild>{children}</DrawerClose>}
          />
        </div>
      </DrawerContent>
    </Drawer>
  );
}

interface ExperienceSummaryProps {
  experience: ProjectExperience;
  /** Dialog·Drawer가 접근성 이름으로 쓰는 제목 */
  Title: ComponentType<{ className?: string; children: ReactNode }>;
  closeButton: (children: ReactNode) => ReactNode;
}

// ASSUMPTION: 프로토타입에 경험 요약 모달이 없어, 자료 요약 모달 모양에 경험 목록 응답에 있는 값(분류, 제목, 한 일, 출처)만 담는다.
function ExperienceSummary({ experience, Title, closeButton }: ExperienceSummaryProps) {
  return (
    <div className="flex flex-col gap-8 text-gray-900">
      <div className="flex flex-col gap-3.5">
        <div className="flex items-center gap-2 text-sm font-medium text-gray-500">
          {experience.category}
          {closeButton(
            <button
              type="button"
              aria-label="닫기"
              className="focus-visible:outline-primary-600 ml-auto shrink-0 rounded-sm text-gray-500 hover:text-gray-900 focus-visible:outline-2"
            >
              <IconX className="size-5" aria-hidden="true" />
            </button>,
          )}
        </div>
        <Title className="text-2xl leading-snug font-extrabold tracking-tight">
          {experience.title}
        </Title>
      </div>

      {experience.highlights.length > 0 && (
        <section>
          <h3 className="mb-3 text-lg font-bold tracking-tight">한 일</h3>
          <ul className="flex flex-col gap-2.5">
            {experience.highlights.map((highlight) => (
              <li key={highlight} className="flex gap-2.5 text-base leading-relaxed text-gray-700">
                {/* DESIGN-TOKEN: 점을 글자 첫 줄 가운데에 맞추려고 줄 높이에서 점 크기를 뺀 절반만큼 내린다. */}
                <span
                  aria-hidden="true"
                  className="mt-[calc((1lh-4px)/2)] size-1 shrink-0 rounded-full bg-gray-400"
                />
                <span className="min-w-0">{highlight}</span>
              </li>
            ))}
          </ul>
        </section>
      )}

      {experience.sources.length > 0 && (
        <section>
          <h3 className="mb-3 text-lg font-bold tracking-tight">
            출처 <span className="font-medium text-gray-400">{experience.sources.length}</span>
          </h3>
          <ul className="flex flex-col rounded-xl border border-gray-200 p-1.5">
            {experience.sources.map((source, index) => (
              <li key={index}>
                <SourceLink source={source} />
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}
