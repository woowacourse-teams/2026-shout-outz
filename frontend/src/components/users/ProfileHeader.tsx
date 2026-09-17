/**
 * 프로필 상단. 아바타 자리, 이름, 소속 배지, 소개, GitHub·블로그 링크를 보여준다.
 *
 * 소속 배지 문구는 cohort와 track으로 `formatCrewRole`이 만든다.
 * 아바타 이미지는 아직 붙이지 않고 빈 원으로 둔다(응답에 avatarImageId만 온다).
 * 디자인의 "프로필 수정" 버튼은 본인 프로필에서만 필요해 이번 공개 프로필에는 두지 않는다.
 */
export interface ProfileHeaderProps {
  displayName: string;
  cohort: number | null;
  track: string | null;
  bio: string | null;
  githubProfileUrl: string | null;
  blogUrl: string | null;
}

export function ProfileHeader(props: ProfileHeaderProps) {
  return null;
}
