import '@tanstack/react-router';
import type { EnsureQueryDataOptions } from '@tanstack/react-query';

declare module '@tanstack/react-router' {
  interface StaticDataRouteOption {
    ssr?: boolean;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ssrQueries?: (params: Record<string, string>) => EnsureQueryDataOptions<any, any, any, any>[];
  }
}
