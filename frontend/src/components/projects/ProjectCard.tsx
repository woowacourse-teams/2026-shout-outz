import { useEffect, useRef, useState } from 'react';
import { Avatar } from '@/components/Avatar';
import { Badge } from '@/components/Badge';
import {
  IconHeart,
  IconHeartFilled,
  IconClock,
  IconAlertTriangle,
  IconMessageCircle,
} from '@tabler/icons-react';
import { type ProjectTechTag } from '@/types/project';
import type { ProjectApprovalStatus } from '@/types/api';

/**
 * 프로젝트 카드. 프로젝트 목록과 프로필 프로젝트 탭이 같이 쓴다.
 *
 * 응답 객체가 아니라 화면에 그릴 값만 받는다.
 * 목록 응답과 프로필 탭 응답의 members 모양이 조금 달라(프로필 탭에는 userId가 없다)
 * 카드는 두 쪽 모두에 있는 필드만 받는다.
 */
export interface ProjectCardProps {
  title: string;
  tagline: string;
  cohort?: number | null;
  approvalStatus?: ProjectApprovalStatus;
  rejectReason?: string | null;
  likeCount: number;
  likedByMe?: boolean;
  commentCount: number;
  techTags: ProjectTechTag[];
  thumbnailUrl?: string | null;
  /** 작은 카드에서는 참여자를 아바타로만 보여준다. */
  members: { displayName: string; avatarUrl?: string | null }[];
}

export function ProjectCard({
  title,
  tagline,
  cohort,
  approvalStatus,
  rejectReason,
  likeCount,
  likedByMe = false,
  commentCount,
  techTags,
  thumbnailUrl,
  members,
}: ProjectCardProps) {
  return (
    <article className="relative min-w-0">
      <div>
        <div className="relative flex aspect-video items-center justify-center overflow-hidden rounded-xl bg-gray-100">
          {thumbnailUrl ? (
            <img
              src={thumbnailUrl}
              alt={`${title} 썸네일`}
              loading="lazy"
              className="size-full object-cover"
            />
          ) : (
            <span
              aria-hidden="true"
              className="px-4 text-center text-sm font-semibold text-gray-500"
            >
              {title}
            </span>
          )}
          {(approvalStatus === 'PENDING' || approvalStatus === 'REJECTED') && (
            <div className="absolute inset-x-3 top-3 flex flex-col items-start gap-1">
              {approvalStatus === 'PENDING' ? (
                <Badge
                  variant="solid"
                  tone="gray"
                  role="status"
                  className="gap-1.5 bg-yellow-100 px-2.5 py-1.5 text-xs leading-4 text-yellow-800"
                >
                  <IconClock className="size-3.5 shrink-0" aria-hidden="true" />
                  승인 대기
                </Badge>
              ) : (
                <Badge
                  variant="solid"
                  tone="gray"
                  role="status"
                  className="max-w-full flex-col items-start gap-1 bg-red-500 px-2.5 py-1.5 text-xs leading-4 whitespace-normal text-white"
                >
                  <span className="inline-flex items-center gap-1.5">
                    <IconAlertTriangle className="size-3.5 shrink-0" aria-hidden="true" />
                    승인 반려
                  </span>
                  {rejectReason && (
                    <span className="text-xs leading-relaxed font-normal wrap-break-word">
                      {rejectReason}
                    </span>
                  )}
                </Badge>
              )}
            </div>
          )}
        </div>

        {/* 섹션마다 높이를 고정해 내용 길이와 상관없이 카드 높이가 같게 한다. 넘치면 말줄임한다. */}
        <p className="mt-3 h-4 text-xs leading-4 text-gray-500">
          {cohort != null && `우아한테크코스 ${cohort}기`}
        </p>
        <h2 className="mt-2 truncate text-lg font-bold text-gray-900" title={title}>
          {title}
        </h2>
        <p className="mt-2 line-clamp-2 h-[2lh] text-sm leading-relaxed wrap-break-word text-gray-600">
          {tagline}
        </p>

        <TechTagList techTags={techTags} />

        <div className="mt-4 flex items-center justify-between gap-2">
          <ul className="flex items-center -space-x-2">
            {members.map((member, index) => (
              <li key={`${member.displayName}-${index}`}>
                <Avatar
                  size="xs"
                  src={member.avatarUrl}
                  name={member.displayName}
                  alt={member.displayName}
                  className="ring-2 ring-white"
                />
              </li>
            ))}
          </ul>

          <p className="flex items-center gap-3 text-xs text-gray-500">
            <span>
              {likedByMe ? (
                <IconHeartFilled className="text-primary-600 inline size-3.5" aria-hidden="true" />
              ) : (
                <IconHeart className="inline size-3.5" aria-hidden="true" />
              )}{' '}
              <span aria-label="좋아요 수">{likeCount}</span>
            </span>
            <span>
              <IconMessageCircle className="inline size-3.5" aria-hidden="true" />{' '}
              <span aria-label="댓글 수">{commentCount}</span>
            </span>
          </p>
        </div>
      </div>
    </article>
  );
}

/** 기술 태그를 보여줄 줄 수. */
const TECH_TAG_ROWS = 2;

/**
 * 기술 태그를 `TECH_TAG_ROWS`줄까지 보여주고, 넘치는 태그는 `+N` 배지로 줄인다.
 *
 * 배지는 글자가 아니라 `line-clamp`로 말줄임이 되지 않는다.
 * 그래서 목록 폭이 바뀔 때마다 태그 폭을 재서 줄에 채워 보고, 들어가는 개수만 보여준다.
 * 숨긴 태그도 폭을 재야 하므로 DOM에는 남기고 `invisible absolute`로 흐름에서만 뺀다.
 */
function TechTagList({ techTags }: { techTags: ProjectTechTag[] }) {
  const listRef = useRef<HTMLUListElement>(null);
  const moreRef = useRef<HTMLSpanElement>(null);
  const [fittingCount, setFittingCount] = useState(techTags.length);
  const visibleCount = Math.min(fittingCount, techTags.length);
  const hiddenCount = techTags.length - visibleCount;

  useEffect(() => {
    const list = listRef.current;
    if (!list || typeof ResizeObserver === 'undefined') return;

    const observer = new ResizeObserver(() =>
      setFittingCount(countFittingTags(list, techTags.length, moreRef.current?.offsetWidth ?? 0)),
    );
    observer.observe(list);
    return () => observer.disconnect();
  }, [techTags]);

  // 배지 높이 20px(text-xs 16px + py-0.5) × 2줄 + 줄 간격 6px(gap-1.5) = 46px.
  // li를 flex로 둬야 inline-flex 배지 아래 글자 기준선 여백이 빠져 한 줄이 정확히 20px이 된다.
  if (techTags.length === 0) return <div className="mt-3 h-[2.875rem]" />;

  return (
    <div className="relative mt-3 h-[2.875rem] overflow-hidden">
      <ul
        ref={listRef}
        aria-label="기술 스택"
        className="relative flex flex-wrap content-start gap-1.5"
      >
        {techTags.map((tag, index) => (
          <li key={tag.id} className={index < visibleCount ? 'flex' : 'invisible absolute flex'}>
            <Badge>{tag.displayName}</Badge>
          </li>
        ))}
        {hiddenCount > 0 && (
          <li className="flex">
            <Badge>
              <span aria-hidden="true">+{hiddenCount}</span>
              <span className="sr-only">{`기술 스택 ${hiddenCount}개 더 있음`}</span>
            </Badge>
          </li>
        )}
      </ul>
      {/* +N 배지 폭을 재는 용도. 자리수가 가장 긴 경우로 재서 배지가 줄을 넘지 않게 한다. */}
      <span ref={moreRef} aria-hidden="true" className="invisible absolute top-0 left-0">
        <Badge>+{techTags.length}</Badge>
      </span>
    </div>
  );
}

/** 태그를 왼쪽부터 줄에 채워 보며, 넘칠 때는 `+N` 배지까지 `TECH_TAG_ROWS`줄 안에 들어가는 태그 수를 찾는다. */
function countFittingTags(list: HTMLUListElement, total: number, moreWidth: number) {
  const tagWidths = [...list.children]
    .slice(0, total)
    .map((item) => (item as HTMLElement).offsetWidth);
  const gap = parseFloat(getComputedStyle(list).columnGap) || 0;
  const maxWidth = list.clientWidth;

  const fits = (widths: number[]) => {
    let rows = 1;
    let lineWidth = 0;
    for (const width of widths) {
      if (lineWidth > 0 && lineWidth + gap + width > maxWidth) {
        rows += 1;
        lineWidth = width;
        if (rows > TECH_TAG_ROWS) return false;
      } else {
        lineWidth = lineWidth === 0 ? width : lineWidth + gap + width;
      }
    }
    return true;
  };

  if (fits(tagWidths)) return total;
  for (let count = total - 1; count > 0; count -= 1) {
    if (fits([...tagWidths.slice(0, count), moreWidth])) return count;
  }
  return 0;
}
