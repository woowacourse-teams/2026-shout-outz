import { AuthSheet } from '@/components/auth/AuthSheet';
import { useModal } from '@/hooks/useModal';
import { useSession } from '@/hooks/useSession';

export function useRequireAuthentication() {
  const { open } = useModal();
  const { data: session } = useSession();
  const authenticated = session?.status === 'AUTHENTICATED' && session.userId != null;

  const requireAuthentication = () => {
    if (authenticated) return true;
    void open<void>((close) => <AuthSheet onClose={() => close()} />);
    return false;
  };

  return { authenticated, requireAuthentication };
}
