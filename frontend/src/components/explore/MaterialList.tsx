import { useRouter } from '@tanstack/react-router';
import type { ReactNode } from 'react';

import type { Material } from '@/apis/explore';
import { Badge } from '@/components/Badge';
import { openMaterialSummary } from '@/components/explore/MaterialSummaryModal';
import {
  formatMaterialDate,
  MaterialIcon,
  MaterialMeta,
  techTalkThumbnailOf,
} from '@/components/explore/MaterialParts';
import { analytics } from '@/utils/analytics';
import { cn } from '@/utils/cn';

/** 목록에서 자료를 고르는 동안의 상태. 프로젝트 상세 자료 탭의 "필요 없는 자료 고르기"가 쓴다. */
export interface MaterialSelection {
  /** 고를 수 있는 자료인지 */
  canSelect: (material: Material) => boolean;
  selectedIds: ReadonlySet<number>;
  onToggle: (resourceId: number) => void;
}

interface MaterialListProps {
  materials: Material[];
  /** 한 줄 소개 아래에 덧붙일 내용 */
  renderExtra?: (material: Material) => ReactNode;
  /** 있으면 고를 수 있는 자료 앞에 체크박스를 두고, 누르면 요약 대신 고른다. */
  selection?: MaterialSelection;
}

/** 블로그·PR·팀 문서 목록. 항목을 누르면 자료 요약을 연다. */
export function MaterialList({ materials, renderExtra, selection }: MaterialListProps) {
  const router = useRouter();
  const itemClass =
    'focus-visible:outline-primary-600 group flex w-full cursor-pointer flex-col gap-2.5 py-6 text-left focus-visible:outline-2';

  return (
    <ul className="flex flex-col">
      {materials.map((material) => {
        const content = (
          <>
            <MaterialMeta material={material} />
            <h3 className="flex items-start gap-2 text-lg leading-snug font-bold tracking-tight text-gray-900 group-hover:underline">
              <MaterialIcon material={material} className="mt-[calc((1lh-18px)/2)]" />
              <span className="min-w-0">{material.title}</span>
            </h3>
            {material.summary && (
              <p className="text-base leading-relaxed text-gray-800">{material.summary}</p>
            )}
            {renderExtra?.(material)}
          </>
        );

        if (selection?.canSelect(material)) {
          const checked = selection.selectedIds.has(material.resourceId);
          return (
            <li key={material.resourceId} className="border-t border-gray-100">
              <label className={cn(itemClass, 'flex-row items-start gap-3')}>
                <input
                  type="checkbox"
                  checked={checked}
                  onChange={() => selection.onToggle(material.resourceId)}
                  className="accent-primary-600 mt-1 size-4 shrink-0 cursor-pointer"
                />
                <span className="flex min-w-0 flex-1 flex-col gap-2.5">{content}</span>
              </label>
            </li>
          );
        }

        return (
          <li key={material.resourceId} className="border-t border-gray-100">
            <button
              type="button"
              className={itemClass}
              onClick={() => openMaterialSummary(material, router)}
            >
              {content}
            </button>
          </li>
        );
      })}
    </ul>
  );
}

/** 테코톡 카드 목록. 카드를 누르면 영상으로 바로 이동한다. */
export function TechTalkList({ talks, columns }: { talks: Material[]; columns: 1 | 3 }) {
  return (
    <ul
      className={cn(
        'grid grid-cols-1 gap-x-4 gap-y-6',
        columns === 3 && 'sm:grid-cols-2 md:grid-cols-3',
      )}
    >
      {talks.map((talk) => {
        const thumbnailUrl = techTalkThumbnailOf(talk.url);
        return (
          <li key={talk.resourceId} className="min-w-0">
            <a
              href={talk.url}
              onClick={() =>
                analytics.track({ name: 'tech_talk_clicked', resourceId: talk.resourceId })
              }
              target="_blank"
              rel="noopener noreferrer"
              className="focus-visible:outline-primary-600 group block rounded-xl focus-visible:outline-2"
            >
              <div className="aspect-video overflow-hidden rounded-xl bg-gray-100">
                {thumbnailUrl && (
                  <img
                    src={thumbnailUrl}
                    alt=""
                    loading="lazy"
                    className="size-full object-cover transition-transform group-hover:scale-[1.02]"
                  />
                )}
              </div>
              <p className="mt-2 flex items-center gap-1.5">
                <Badge tone="green">테코톡</Badge>
                <span className="truncate text-xs text-gray-400">
                  {formatMaterialDate(talk.publishedAt)}
                </span>
              </p>
              <p className="mt-1 line-clamp-2 text-sm leading-snug font-semibold text-gray-900 group-hover:underline">
                {talk.title}
              </p>
            </a>
          </li>
        );
      })}
    </ul>
  );
}
