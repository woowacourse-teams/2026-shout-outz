import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';

import { sessionQuery } from '@/apis/session';
import { myProfileSummaryQuery } from '@/apis/user';
import { analytics } from '@/utils/analytics';

export function AnalyticsIdentifier() {
  const { data: session } = useQuery({ ...sessionQuery, throwOnError: false });
  // 스키마상 userId가 없을 수 있어 둘 다 확인한다.
  const userId = session?.status === 'AUTHENTICATED' ? (session.userId ?? null) : null;

  const { data: profile } = useQuery({
    ...myProfileSummaryQuery(userId ?? 0),
    enabled: userId !== null,
    throwOnError: false,
  });

  useEffect(() => {
    if (userId === null) {
      analytics.identify(null);
      return;
    }
    if (!profile) return;

    analytics.identify({ userId, handle: profile.handle });
  }, [userId, profile]);

  return null;
}
