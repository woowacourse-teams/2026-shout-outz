import { useQuery } from '@tanstack/react-query';

import { sessionQuery } from '@/apis/session';

/**
 * 로그인한 사용자가 이 프로젝트 팀원인지. 자료 제보는 팀원만 보낼 수 있어 제보 링크를 보여 줄지 정한다.
 * 세션의 userId가 상세 응답 members[].userId에 있으면 팀원이다. 세션을 받기 전과 비로그인은 false다.
 */
export function useIsProjectMember(memberUserIds: (number | null | undefined)[]) {
  const { data: session } = useQuery(sessionQuery);
  if (session?.status !== 'AUTHENTICATED' || session.userId == null) return false;
  return memberUserIds.includes(session.userId);
}
