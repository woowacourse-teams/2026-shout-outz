import type { ReactNode } from 'react';

import type { Material, MaterialKind } from '@/apis/explore';
import { Badge, type BadgeTone } from '@/components/Badge';
import { cn } from '@/utils/cn';

export const MATERIAL_KIND_LABELS: Record<MaterialKind, [label: string, tone: BadgeTone]> = {
  BLOG: ['블로그', 'primary'],
  PR: ['PR', 'primary'],
  TEAM_DOC: ['팀 문서', 'green'],
  TECH_TALK: ['테코톡', 'green'],
};

/** 2026-10-20 → 2026년 10월 20일. 시간대에 따라 날짜가 바뀌지 않게 Date로 바꾸지 않는다. */
export function formatMaterialDate(value: string | null) {
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value ?? '');
  return match ? `${match[1]}년 ${Number(match[2])}월 ${Number(match[3])}일` : '';
}

export function hostnameOf(url: string) {
  try {
    return new URL(url).hostname;
  } catch {
    return url;
  }
}

/** 종류 배지 · 팀 · 날짜 한 줄 */
export function MaterialMeta({ material, trailing }: { material: Material; trailing?: ReactNode }) {
  const [label, tone] = MATERIAL_KIND_LABELS[material.kind];

  return (
    <span className="flex min-w-0 items-center gap-2 text-sm text-gray-400">
      <Badge tone={tone}>{label}</Badge>
      {material.project && (
        <span className="inline-flex min-w-0 items-center gap-1.5 font-semibold text-gray-600">
          <ProjectLogo project={material.project} />
          <span className="truncate">{material.project.name}</span>
        </span>
      )}
      <span className="shrink-0">{formatMaterialDate(material.publishedAt)}</span>
      {trailing}
    </span>
  );
}

function ProjectLogo({ project }: { project: NonNullable<Material['project']> }) {
  if (project.logoUrl)
    return (
      <img
        src={project.logoUrl}
        alt=""
        width={16}
        height={16}
        className="size-4 shrink-0 rounded-sm object-cover"
        loading="lazy"
      />
    );
  // 팀 로고가 없으면 회색 사각형으로 자리를 잡는다.
  return (
    <span
      aria-hidden="true"
      className="ring-gray-opacity-100 size-4 shrink-0 rounded-sm bg-gray-200 ring-1 ring-inset"
    />
  );
}

/**
 * 제목 앞 출처 아이콘. 블로그는 파비콘, PR과 팀 문서는 GitHub 마크에 종류 배지를 붙인다.
 * `size`는 제목 옆(18px)과 원문 카드(22px)에서 다르게 쓴다.
 */
export function MaterialIcon({
  material,
  size = 'md',
  className,
}: {
  material: Material;
  size?: 'md' | 'lg';
  className?: string;
}) {
  const box = size === 'md' ? 'size-4.5' : 'size-5.5';

  if (material.kind === 'PR' || material.kind === 'TEAM_DOC') {
    return (
      <span
        role="img"
        aria-label={material.kind === 'PR' ? 'GitHub PR' : 'GitHub 팀 문서'}
        className={cn('relative inline-flex shrink-0 text-gray-900', box, className)}
      >
        <GithubMark className={box} />
        <span className="bg-background absolute -right-1 -bottom-1 flex size-3 items-center justify-center rounded-full ring-1 ring-gray-200">
          {material.kind === 'PR' ? (
            // DESIGN-TOKEN: 12px 배지 안에 넣는 6px 글자는 토큰에 없는 크기다.
            <span className="text-primary-600 text-[6px] font-extrabold">PR</span>
          ) : (
            <DocumentMark className="size-2 text-green-600" />
          )}
        </span>
      </span>
    );
  }

  if (material.faviconUrl)
    return (
      <img
        src={material.faviconUrl}
        alt=""
        className={cn('shrink-0 rounded-sm', box, className)}
        loading="lazy"
      />
    );

  // 파비콘을 모르는 블로그는 회색 사각형으로 자리를 잡는다.
  return (
    <span
      aria-hidden="true"
      className={cn(
        'ring-gray-opacity-100 shrink-0 rounded-sm bg-gray-200 ring-1 ring-inset',
        box,
        className,
      )}
    />
  );
}

function GithubMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="currentColor" aria-hidden="true" className={className}>
      <path d="M8 0c4.42 0 8 3.58 8 8a8.013 8.013 0 0 1-5.45 7.59c-.4.08-.55-.17-.55-.38 0-.27.01-1.13.01-2.2 0-.75-.25-1.23-.54-1.48 1.78-.2 3.65-.88 3.65-3.95 0-.88-.31-1.59-.82-2.15.08-.2.36-1.02-.08-2.12 0 0-.67-.22-2.2.82-.64-.18-1.32-.27-2-.27-.68 0-1.36.09-2 .27-1.53-1.03-2.2-.82-2.2-.82-.44 1.1-.16 1.92-.08 2.12-.51.56-.82 1.28-.82 2.15 0 3.06 1.86 3.75 3.64 3.95-.23.2-.44.55-.51 1.07-.46.21-1.61.55-2.33-.66-.15-.24-.6-.83-1.23-.82-.67.01-.27.38.01.53.34.19.73.9.82 1.13.16.45.68 1.31 2.69.94 0 .67.01 1.3.01 1.49 0 .21-.15.45-.55.38A7.995 7.995 0 0 1 0 8c0-4.42 3.58-8 8-8Z" />
    </svg>
  );
}

function DocumentMark({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className={className}
    >
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6M8 13h8M8 17h6" />
    </svg>
  );
}
