import type { ComponentProps } from 'react';

import { Gnb } from '@/components/Gnb';
import { AuthActions } from '@/components/auth/AuthActions';

export function AppGnb(props: Omit<ComponentProps<typeof Gnb>, 'trailing'>) {
  return <Gnb trailing={<AuthActions />} {...props} />;
}
