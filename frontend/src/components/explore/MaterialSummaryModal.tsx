import { useQuery } from '@tanstack/react-query';
import { IconArrowUpRight, IconX } from '@tabler/icons-react';
import { Link, RouterContextProvider, type AnyRouter } from '@tanstack/react-router';
import { overlay } from 'overlay-kit';
import type { ComponentProps, ComponentType, ReactNode } from 'react';

import { materialDetailQuery, type Material, type MaterialDetail } from '@/apis/explore';
import {
  hostnameOf,
  isGithubMaterial,
  isPullRequest,
  isTechTalk,
  MaterialIcon,
  MaterialMeta,
} from '@/components/explore/MaterialParts';
import { Badge } from '@/components/Badge';
import { getButtonStyles } from '@/components/Button';
import { Dialog, DialogClose, DialogContent, DialogTitle } from '@/components/ui/dialog';
import { Drawer, DrawerClose, DrawerContent, DrawerTitle } from '@/components/ui/drawer';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { cn } from '@/utils/cn';
import { getApiErrorMessage } from '@/utils/error';

/**
 * 블로그·PR·팀 문서 자료의 요약을 데스크톱은 모달, 모바일은 바텀시트로 연다.
 * overlay-kit은 라우터 바깥에 그려서, 자료 정리 보기 링크가 동작하도록 연 화면의 라우터를 넘겨받는다.
 */
export function openMaterialSummary(material: Material, router: AnyRouter) {
  overlay.open(({ isOpen, close, unmount }) => (
    <RouterContextProvider router={router}>
      <MaterialSummaryModal material={material} isOpen={isOpen} onClose={close} onExit={unmount} />
    </RouterContextProvider>
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
          // DESIGN-TOKEN: 화면 가운데에서 위아래로 56px씩 띄운 높이 안에서 스크롤하도록 계산식으로 높이를 제한한다.
          // 바깥은 모서리를 자르기만 하고 스크롤은 안쪽에서 한다. 둥근 요소 자체가 스크롤되면 스크롤바가 오른쪽 모서리를 덮어 직각으로 보인다.
          className="flex max-h-[calc(100dvh-7rem)] flex-col overflow-hidden rounded-2xl shadow-2xl sm:max-w-150"
        >
          <div className="overflow-y-auto px-7 pt-7 pb-10">
            <MaterialSummary
              material={material}
              Title={DialogTitle}
              trailing={
                <DialogClose asChild>
                  <CloseButton />
                </DialogClose>
              }
            />
            <MaterialDetailLink
              material={material}
              onNavigate={() => {
                onClose();
                onExit();
              }}
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
        <div className="overflow-y-auto px-5 pt-3.5 pb-8">
          <MaterialSummary
            material={material}
            Title={DrawerTitle}
            trailing={
              <DrawerClose asChild>
                <CloseButton />
              </DrawerClose>
            }
          />
          <MaterialDetailLink material={material} onNavigate={onClose} />
        </div>
      </DrawerContent>
    </Drawer>
  );
}

interface MaterialSummaryProps {
  material: Material;
  /**
   * 제목 요소. 모달·바텀시트에서는 Dialog·Drawer가 접근성 이름으로 쓰는 제목을 넘긴다. asChild로 감싸면 기본 클래스(text-lg)가
   * 병합되지 않고 이어 붙어 덮어쓰지 못하므로 className을 직접 넘긴다.
   */
  Title: ComponentType<{ className?: string; children: ReactNode }>;
  /** 메타 줄 오른쪽 끝. 모달에서는 닫기 버튼을 둔다. */
  trailing?: ReactNode;
  /** 제목 아래 원문 카드 모양. 모달은 썸네일 카드, 자료 정리 보기 페이지는 원문 보기 버튼이 있는 카드 */
  variant?: 'modal' | 'page';
}

/** 자료 요약 내용. 자료 요약 모달·바텀시트와 자료 정리 보기 페이지가 같이 쓴다. */
export function MaterialSummary({
  material,
  Title,
  trailing,
  variant = 'modal',
}: MaterialSummaryProps) {
  const detail = useQuery(materialDetailQuery(material.resourceId));

  return (
    <div className={cn('flex flex-col text-gray-900', variant === 'page' ? 'gap-10' : 'gap-8')}>
      <div className="flex flex-col gap-3.5">
        <MaterialMeta material={material} trailing={trailing} />
        <Title
          className={cn(
            'flex items-start gap-2 leading-snug font-extrabold tracking-tight',
            variant === 'page' ? 'text-2xl md:text-3xl' : 'text-2xl',
          )}
        >
          <MaterialIcon material={material} className="mt-[calc((1lh-18px)/2)]" />
          <span className="min-w-0">{material.title}</span>
        </Title>
        {variant === 'page' ? (
          <SourceCard material={material} />
        ) : (
          <SourcePreviewCard
            material={material}
            thumbnailUrl={detail.data?.linkPreview?.imageUrl ?? null}
          />
        )}
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

function CloseButton(props: ComponentProps<'button'>) {
  return (
    <button
      type="button"
      aria-label="닫기"
      className="focus-visible:outline-primary-600 ml-auto shrink-0 rounded-sm text-gray-500 hover:text-gray-900 focus-visible:outline-2"
      {...props}
    >
      <IconX className="size-5" aria-hidden="true" />
    </button>
  );
}

/** 모달·바텀시트 내용 맨 끝의 자료 정리 보기 버튼. 스크롤을 끝까지 내리면 보인다. */
function MaterialDetailLink({
  material,
  onNavigate,
}: {
  material: Material;
  /** 자료 정리 보기로 이동할 때 모달을 닫는다. */
  onNavigate: () => void;
}) {
  return (
    <Link
      to="/materials/$materialId"
      params={{ materialId: String(material.resourceId) }}
      onClick={onNavigate}
      className={getButtonStyles({ variant: 'outline', size: 'lg', className: 'mt-10 w-full' })}
    >
      상세 보기
    </Link>
  );
}

/** 모달·바텀시트의 원문 카드. 누르면 원문을 새 탭으로 연다. */
function SourcePreviewCard({
  material,
  thumbnailUrl,
}: {
  material: Material;
  /** 원문 페이지의 대표 이미지(링크 미리보기). 없으면 회색 칸 */
  thumbnailUrl: string | null;
}) {
  return (
    <a
      href={material.url}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={`원문 보기: ${material.title}`}
      className="focus-visible:outline-primary-600 bg-background flex overflow-hidden rounded-lg border border-gray-200 transition-colors hover:bg-gray-50 focus-visible:outline-2"
    >
      {thumbnailUrl ? (
        <img
          src={thumbnailUrl}
          alt=""
          width={80}
          height={80}
          loading="lazy"
          className="size-20 shrink-0 object-cover"
        />
      ) : (
        <span aria-hidden="true" className="size-20 shrink-0 bg-gray-100" />
      )}
      <span className="flex min-w-0 flex-1 flex-col justify-center gap-1 border-l border-gray-200 px-4 py-3">
        <span className="truncate text-sm font-bold text-gray-900">{material.title}</span>
        <span className="flex min-w-0 items-center gap-1.5 text-xs text-gray-500">
          <MaterialIcon material={material} />
          <span className="truncate">{hostnameOf(material.url)}</span>
        </span>
      </span>
    </a>
  );
}

const sourceLabelOf = (material: Material) => {
  if (isPullRequest(material)) return 'GitHub PR · 원문';
  if (isGithubMaterial(material)) return 'GitHub 팀 문서 · 원문';
  if (isTechTalk(material)) return '테코톡 · 원문';
  return '블로그 · 원문';
};

/** 자료 정리 보기 페이지의 원문 카드 */
function SourceCard({ material }: { material: Material }) {
  const host = hostnameOf(material.url);
  const path = material.url.slice(material.url.indexOf(host) + host.length);

  return (
    <div className="flex items-center gap-3.5 rounded-xl border border-gray-200 bg-gray-50 px-4 py-3.5">
      <span className="bg-background flex size-11 shrink-0 items-center justify-center rounded-lg ring-1 ring-gray-200 ring-inset">
        <MaterialIcon material={material} size="lg" />
      </span>
      <span className="flex min-w-0 flex-1 flex-col gap-0.5 text-xs">
        <span className="text-gray-500">{sourceLabelOf(material)}</span>
        <span className="truncate text-sm font-bold text-gray-900">{material.title}</span>
        <span className="truncate text-gray-600">
          {host}
          <span className="text-gray-400">{path}</span>
        </span>
      </span>
      <a
        href={material.url}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={`원문 보기: ${material.title}`}
        className="focus-visible:outline-primary-600 inline-flex h-9 shrink-0 items-center gap-1.5 rounded-lg bg-gray-900 px-3.5 text-xs font-bold text-white hover:bg-gray-700 focus-visible:outline-2 focus-visible:outline-offset-2"
      >
        원문 보기
        <IconArrowUpRight className="size-3.5" aria-hidden="true" />
      </a>
    </div>
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
      {detail.keyPoints.length > 0 && (
        <SummarySection title="이 자료에서 배울 점">
          <ol>
            {detail.keyPoints.map((keyPoint, index) => (
              <li
                key={keyPoint}
                className="flex gap-3 border-t border-gray-100 py-3 text-base leading-relaxed text-gray-800"
              >
                <span className="mt-0.5 flex size-5.5 shrink-0 items-center justify-center rounded-full bg-gray-900 text-xs font-bold text-white">
                  {index + 1}
                </span>
                <span>{keyPoint}</span>
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
              {/* 서버는 원인과 결과를 모르면 null로 준다. 그 줄은 빼고 보여 준다. */}
              {experience.cause && (
                <ExperienceRow label="원인" dot="bg-yellow-500">
                  {experience.cause}
                </ExperienceRow>
              )}
              <ExperienceRow label="해결" dot="bg-primary-500">
                {experience.solution}
              </ExperienceRow>
              {experience.result && (
                <ExperienceRow label="결과" dot="bg-green-500" emphasis>
                  {experience.result}
                </ExperienceRow>
              )}
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
