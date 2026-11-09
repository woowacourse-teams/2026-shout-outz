import { useQuery } from '@tanstack/react-query';
import { IconChevronRight, IconX } from '@tabler/icons-react';
import { overlay } from 'overlay-kit';
import type { ComponentType, ReactNode } from 'react';

import { materialDetailQuery, type Material, type MaterialDetail } from '@/apis/explore';
import { hostnameOf, MaterialIcon, MaterialMeta } from '@/components/explore/MaterialParts';
import { Badge } from '@/components/Badge';
import { Dialog, DialogClose, DialogContent, DialogTitle } from '@/components/ui/dialog';
import { Drawer, DrawerClose, DrawerContent, DrawerTitle } from '@/components/ui/drawer';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { getApiErrorMessage } from '@/utils/error';

/** 블로그·PR·팀 문서 자료의 요약을 데스크톱은 모달, 모바일은 바텀시트로 연다. */
export function openMaterialSummary(material: Material) {
  overlay.open(({ isOpen, close, unmount }) => (
    <MaterialSummaryModal material={material} isOpen={isOpen} onClose={close} onExit={unmount} />
  ));
}

interface MaterialSummaryModalProps {
  material: Material;
  isOpen: boolean;
  onClose: () => void;
  /** 닫힌 뒤 DOM에서 지울 때 */
  onExit: () => void;
}

export function MaterialSummaryModal({
  material,
  isOpen,
  onClose,
  onExit,
}: MaterialSummaryModalProps) {
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
          // DESIGN-TOKEN: 화면 위아래로 56px씩 띄운 높이 안에서 스크롤하도록 계산식으로 높이를 제한한다.
          // 바깥은 모서리를 자르기만 하고 스크롤은 안쪽에서 한다. 둥근 요소 자체가 스크롤되면 스크롤바가 오른쪽 모서리를 덮어 직각으로 보인다.
          className="top-14 flex max-h-[calc(100dvh-7rem)] translate-y-0 flex-col overflow-hidden rounded-2xl shadow-2xl sm:max-w-150"
        >
          <div className="overflow-y-auto px-7 pt-7 pb-12">
            <MaterialSummary
              material={material}
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
          <MaterialSummary
            material={material}
            Title={DrawerTitle}
            closeButton={(children) => <DrawerClose asChild>{children}</DrawerClose>}
          />
        </div>
      </DrawerContent>
    </Drawer>
  );
}

interface MaterialSummaryProps {
  material: Material;
  /**
   * Dialog·Drawer가 접근성 이름으로 쓰는 제목. asChild로 감싸면 기본 클래스(text-lg)가
   * 병합되지 않고 이어 붙어 덮어쓰지 못하므로 className을 직접 넘긴다.
   */
  Title: ComponentType<{ className?: string; children: ReactNode }>;
  closeButton: (children: ReactNode) => ReactNode;
}

function MaterialSummary({ material, Title, closeButton }: MaterialSummaryProps) {
  const detail = useQuery(materialDetailQuery(material.id));

  return (
    <div className="flex flex-col gap-8 text-gray-900">
      <div className="flex flex-col gap-3.5">
        <MaterialMeta
          material={material}
          trailing={closeButton(
            <button
              type="button"
              aria-label="닫기"
              className="focus-visible:outline-primary-600 ml-auto shrink-0 rounded-sm text-gray-500 hover:text-gray-900 focus-visible:outline-2"
            >
              <IconX className="size-5" aria-hidden="true" />
            </button>,
          )}
        />
        <Title className="flex items-start gap-2 text-2xl leading-snug font-extrabold tracking-tight">
          <MaterialIcon material={material} className="mt-[calc((1lh-18px)/2)]" />
          <span className="min-w-0">{material.title}</span>
        </Title>
        <SourceCard material={material} />
      </div>

      {detail.data && detail.data.techTags.length > 0 && (
        <SummarySection title="기술 태그">
          <p className="flex flex-wrap gap-1.5">
            {detail.data.techTags.map((tag) => (
              <Badge key={tag}>{tag}</Badge>
            ))}
          </p>
        </SummarySection>
      )}

      {material.summary && (
        <SummarySection title="한줄 소개">
          <p className="text-base leading-relaxed font-medium">{material.summary}</p>
        </SummarySection>
      )}

      {detail.isPending && <DetailSkeleton />}
      {detail.isError && (
        <p role="alert" className="text-sm text-gray-500">
          {getApiErrorMessage(detail.error)}
        </p>
      )}
      {detail.data && <DetailSections detail={detail.data} />}
    </div>
  );
}

function SourceCard({ material }: { material: Material }) {
  return (
    <a
      href={material.url}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={`원문 보기: ${material.title}`}
      className="focus-visible:outline-primary-600 flex items-center gap-3 rounded-xl border border-gray-200 py-2.5 pr-3 pl-2.5 transition-colors hover:bg-gray-50 focus-visible:outline-2"
    >
      <span className="flex size-9 shrink-0 items-center justify-center rounded-xl ring-1 ring-gray-200 ring-inset">
        <MaterialIcon material={material} size="lg" />
      </span>
      <span className="flex min-w-0 flex-1 flex-col gap-px">
        <span className="truncate text-sm font-semibold text-gray-900">{material.title}</span>
        <span className="truncate text-xs text-gray-500">{hostnameOf(material.url)}</span>
      </span>
      <IconChevronRight className="size-4.5 shrink-0 text-gray-400" aria-hidden="true" />
    </a>
  );
}

function SummarySection({
  title,
  count,
  children,
}: {
  title: string;
  count?: number;
  children: ReactNode;
}) {
  return (
    <section>
      <h3 className="mb-3 text-lg font-bold tracking-tight">
        {title}
        {count !== undefined && <span className="ml-1 font-medium text-gray-400">{count}</span>}
      </h3>
      {children}
    </section>
  );
}

function DetailSections({ detail }: { detail: MaterialDetail }) {
  return (
    <>
      {detail.learnings.length > 0 && (
        <SummarySection title="이 자료에서 배울 점">
          <ol>
            {detail.learnings.map((learning, index) => (
              <li
                key={learning}
                className="flex gap-3 border-t border-gray-100 py-3 text-base leading-relaxed text-gray-800"
              >
                <span className="mt-0.5 flex size-5.5 shrink-0 items-center justify-center rounded-full bg-gray-900 text-xs font-bold text-white">
                  {index + 1}
                </span>
                <span>{learning}</span>
              </li>
            ))}
          </ol>
        </SummarySection>
      )}

      {detail.experiences.length > 0 && (
        <SummarySection title="어떤 문제를 어떻게 풀었나" count={detail.experiences.length}>
          {detail.experiences.map((experience, index) => (
            <div key={index} className="border-t border-gray-100 py-3.5">
              <p className="mb-1 text-sm font-semibold text-gray-500">경험 {index + 1}</p>
              <ExperienceRow label="문제" dot="bg-red-400">
                {experience.problem}
              </ExperienceRow>
              <ExperienceRow label="원인" dot="bg-yellow-500">
                {experience.cause}
              </ExperienceRow>
              <ExperienceRow label="해결" dot="bg-primary-500">
                {experience.solution}
              </ExperienceRow>
              <ExperienceRow label="결과" dot="bg-green-500" emphasis>
                {experience.result}
              </ExperienceRow>
            </div>
          ))}
        </SummarySection>
      )}
    </>
  );
}

function ExperienceRow({
  label,
  dot,
  emphasis = false,
  children,
}: {
  label: string;
  dot: string;
  emphasis?: boolean;
  children: ReactNode;
}) {
  const tone = emphasis ? 'text-gray-900' : 'text-gray-600';

  return (
    <div className="flex gap-3 py-1.5">
      <span
        className={`inline-flex h-6 w-14 shrink-0 items-center gap-1.5 text-xs font-bold ${tone}`}
      >
        <span aria-hidden="true" className={`size-1.5 rounded-full ${dot}`} />
        {label}
      </span>
      <p className={`text-base leading-relaxed ${tone} ${emphasis ? 'font-medium' : ''}`}>
        {children}
      </p>
    </div>
  );
}

function DetailSkeleton() {
  return (
    <div role="status" aria-label="자료 요약을 불러오는 중" className="flex flex-col gap-3">
      <span className="h-5 w-32 rounded bg-gray-100 motion-safe:animate-pulse" />
      <span className="h-4 w-full rounded bg-gray-100 motion-safe:animate-pulse" />
      <span className="h-4 w-5/6 rounded bg-gray-100 motion-safe:animate-pulse" />
      <span className="h-4 w-4/6 rounded bg-gray-100 motion-safe:animate-pulse" />
    </div>
  );
}
