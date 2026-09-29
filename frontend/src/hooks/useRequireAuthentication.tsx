import { useQuery } from '@tanstack/react-query';

import { sessionQuery } from '@/apis/session';
import { AuthSheet } from '@/components/auth/AuthSheet';
import { useModal } from '@/hooks/useModal';

export function useRequireAuthentication() {
  const { open } = useModal();
  const { data: session } = useQuery({
    ...sessionQuery,
    enabled: typeof window !== 'undefined',
  });
  const authenticated = session?.status === 'AUTHENTICATED' && session.userId != null;

  const requireAuthentication = () => {
    if (authenticated) return true;
    void open<void>((close) => <AuthSheet onClose={() => close()} />);
    return false;
  };

  return { authenticated, requireAuthentication };
}
