import type { Material } from '@/apis/explore';
import { Badge } from '@/components/Badge';
import { openMaterialSummary } from '@/components/explore/MaterialSummaryModal';
import { formatMaterialDate, MaterialIcon, MaterialMeta } from '@/components/explore/MaterialParts';
import { cn } from '@/utils/cn';

/** 블로그·PR·팀 문서 목록. 항목을 누르면 자료 요약을 연다. */
export function MaterialList({ materials }: { materials: Material[] }) {
  return (
    <ul className="flex flex-col">
      {materials.map((material) => (
        <li key={material.id} className="border-t border-gray-100">
          <button
            type="button"
            className="focus-visible:outline-primary-600 group flex w-full cursor-pointer flex-col gap-2.5 py-6 text-left focus-visible:outline-2"
            onClick={() => openMaterialSummary(material)}
          >
            <MaterialMeta material={material} />
            <h3 className="flex items-start gap-2 text-lg leading-snug font-bold tracking-tight text-gray-900 group-hover:underline">
              <MaterialIcon material={material} className="mt-[calc((1lh-18px)/2)]" />
              <span className="min-w-0">{material.title}</span>
            </h3>
            {material.summary && (
              <p className="text-base leading-relaxed text-gray-800">{material.summary}</p>
            )}
          </button>
        </li>
      ))}
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
      {talks.map((talk) => (
        <li key={talk.id} className="min-w-0">
          <a
            href={talk.url}
            target="_blank"
            rel="noopener noreferrer"
            className="focus-visible:outline-primary-600 group block rounded-xl focus-visible:outline-2"
          >
            <div className="aspect-video overflow-hidden rounded-xl bg-gray-100">
              {talk.thumbnailUrl && (
                <img
                  src={talk.thumbnailUrl}
                  alt=""
                  loading="lazy"
                  className="size-full object-cover transition-transform group-hover:scale-[1.02]"
                />
              )}
            </div>
            <p className="mt-2 flex items-center gap-1.5">
              <Badge tone="green">테코톡</Badge>
              <span className="truncate text-xs text-gray-400">
                {[talk.speaker, formatMaterialDate(talk.publishedAt)].filter(Boolean).join(' ')}
              </span>
            </p>
            <p className="mt-1 line-clamp-2 text-sm leading-snug font-semibold text-gray-900 group-hover:underline">
              {talk.title}
            </p>
          </a>
        </li>
      ))}
    </ul>
  );
}
