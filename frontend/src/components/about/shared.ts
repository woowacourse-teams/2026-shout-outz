import type { AnchorHTMLAttributes, ComponentType } from 'react';

export type AboutLinkProps = AnchorHTMLAttributes<HTMLAnchorElement>;
export interface AboutSectionProps {
  LinkComponent: ComponentType<AboutLinkProps>;
}

export const ABOUT_CONTAINER = 'mx-auto w-full max-w-6xl px-5 md:px-8 xl:px-0';
export const ABOUT_HEADING =
  'mb-5 text-3xl leading-snug font-bold tracking-tight text-gray-900 lg:text-4xl';
export const ABOUT_EYEBROW = 'mb-5 text-xs font-semibold tracking-widest text-primary-500';
export const ABOUT_COPY = 'text-sm leading-7 text-gray-700 md:text-base md:leading-8';
export const ABOUT_TEXT_LINK =
  'inline-flex items-center gap-3 text-sm font-semibold text-primary-600 transition-colors hover:text-primary-700 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary-500';
