/**
 * 참여 팀원 선택 필드.
 *
 * TODO 참여 팀원 선택 시트/모달이 구현되면 그것으로 교체한다. 지금은 폼 안에서 이름이나 handle로
 * `GET /api/v1/users/search`를 검색해 고른 크루의 handle을 쌓는다.
 */
export interface MemberFieldProps {
  value: string[];
  onChange: (next: string[]) => void;
  error?: string;
}

export function MemberField(props: MemberFieldProps) {
  return null;
}
