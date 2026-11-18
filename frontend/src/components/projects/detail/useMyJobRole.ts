import { useQuery } from '@tanstack/react-query';

import { sessionQuery } from '@/apis/session';
import { myProfileQuery } from '@/apis/user';
import { useIsClient } from '@/components/ClientOnly';

/** 내 분야(track)를 프로젝트 경험·자료의 직군 값으로 바꾼다. */
const JOB_ROLE_BY_TRACK = {
  BACKEND: 'BE',
  FRONTEND: 'FE',
  ANDROID: 'AN',
} as const;

export type MyJobRole = (typeof JOB_ROLE_BY_TRACK)[keyof typeof JOB_ROLE_BY_TRACK];

/**
 * 로그인한 사용자의 분야에 맞는 직군(BE·FE·AN). 비로그인, 분야 없음, 프로필을 받기 전이면 null이다.
 * 빌드 때 미리 만든 HTML에는 사용자가 없어, 첫 hydration까지는 null로 맞춘다.
 * 프로필은 헤더(AuthActions)가 이미 받는 쿼리라 요청이 늘지 않는다.
 */
export function useMyJobRole(): MyJobRole | null {
  const isClient = useIsClient();
  const { data: session } = useQuery(sessionQuery);
  const authenticated = session?.status === 'AUTHENTICATED' && session.userId != null;
  const { data: profile } = useQuery({
    ...myProfileQuery(session?.userId ?? 0),
    enabled: isClient && authenticated,
  });
  if (!isClient || !authenticated || !profile?.track) return null;
  return JOB_ROLE_BY_TRACK[profile.track];
}
