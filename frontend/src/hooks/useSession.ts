import { useQuery } from '@tanstack/react-query';

import { sessionQuery } from '@/apis/session';

// 서버 렌더(SSG·SSR)에서는 세션을 묻지 않고 pending으로 둡니다. 서버의 요청에는 사용자 쿠키가 없어
// 항상 비로그인으로 응답되므로, 그 결과가 HTML과 하이드레이션 데이터에 실리면 로그인 사용자도
// 비로그인 화면을 받습니다. 그래서 세션은 useSuspenseQuery 대신 이 훅으로 조회합니다.
export function useSession() {
  return useQuery({ ...sessionQuery, enabled: typeof window !== 'undefined' });
}
