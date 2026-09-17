import { type UserProjectCard } from '@/types/user';

/**
 * 프로필 프로젝트 탭의 카드 한 장.
 *
 * 카드 모양은 공용 `components/projects/ProjectCard`를 쓰고, 이 컴포넌트는 프로필 응답을
 * 그 표시용 props로 옮기는 일만 한다.
 *
 * 썸네일은 thumbnailMediaId만 와서 지금은 빈 자리로 둔다. 디자인의 좋아요·조회수와 참여자
 * 아바타는 응답에 없어 넣지 않는다.
 */
export interface ProfileProjectCardProps {
  project: UserProjectCard;
}

export function ProfileProjectCard(props: ProfileProjectCardProps) {
  return null;
}
