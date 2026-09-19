import { Avatar } from '@/components/Avatar';
import { Badge } from '@/components/Badge';
import { type ProjectTechTag } from '@/types/project';

/**
 * 프로젝트 카드. 프로젝트 목록과 프로필 프로젝트 탭이 같이 쓴다.
 *
 * 응답 객체가 아니라 화면에 그릴 값만 받는다.
 * TODO 썸네일 이미지는 아직 연결하지 않고 제목을 대신 보여준다.
 */
export interface ProjectCardProps {
  title: string;
  tagline: string;
  cohort: number | null;
  likeCount: number;
  commentCount: number;
  techTags: ProjectTechTag[];
  /** 참여자는 아바타로만 보여줘 이름과 식별자만 받는다 */
  members: { userId: number | null; displayName: string }[];
}

export function ProjectCard({
  title,
  tagline,
  cohort,
  likeCount,
  commentCount,
  techTags,
  members,
}: ProjectCardProps) {
  return (
    <article className="min-w-0">
      <div className="flex aspect-video items-center justify-center overflow-hidden rounded-xl bg-gray-100">
        <span aria-hidden="true" className="px-4 text-center text-sm font-semibold text-gray-500">
          {title}
        </span>
      </div>

      {cohort !== null && (
        <p className="mt-3 text-xs text-gray-500">{`우아한테크코스 ${cohort}기`}</p>
      )}
      <h2 className="mt-2 text-lg font-bold wrap-break-word text-gray-900">{title}</h2>
      <p className="mt-2 text-sm leading-relaxed wrap-break-word text-gray-600">{tagline}</p>

      {techTags.length > 0 && (
        <ul aria-label="기술 스택" className="mt-3 flex flex-wrap gap-1.5">
          {techTags.map((tag) => (
            <li key={tag.id}>
              <Badge>{tag.displayName}</Badge>
            </li>
          ))}
        </ul>
      )}

      <div className="mt-4 flex items-center justify-between gap-2">
        <ul className="flex items-center -space-x-2">
          {members.map((member, index) => (
            <li key={member.userId ?? `${member.displayName}-${index}`}>
              {/* TODO 참여자 이미지를 이미지 URL로 받아 src에 연결 */}
              <Avatar size="xs" alt={member.displayName} className="ring-2 ring-white" />
            </li>
          ))}
        </ul>

        <p className="flex items-center gap-3 text-xs text-gray-500">
          <span>
            <span aria-hidden="true">♥ </span>
            <span aria-label="좋아요 수">{likeCount}</span>
          </span>
          <span>
            댓글 <span aria-label="댓글 수">{commentCount}</span>
          </span>
        </p>
      </div>
    </article>
  );
}
