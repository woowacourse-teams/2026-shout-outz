import { linkOptions } from '@tanstack/react-router';
import type { ComponentProps, ReactNode } from 'react';

export const GNB_ITEMS = linkOptions([
  { to: '/', activeOptions: { exact: true }, label: '홈' },
  { to: '/feeds', label: '피드' },
  { to: '/projects', label: '프로젝트' },
  { to: '/news', label: '소식' },
]);

export interface GnbProps extends ComponentProps<'header'> {
  trailing?: ReactNode;
}
