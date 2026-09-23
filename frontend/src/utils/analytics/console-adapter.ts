import { type AnalyticsAdapter } from '@/utils/analytics/types';

export const consoleAdapter = (): AnalyticsAdapter => ({
  name: 'console',
  pageView: (path, context) => console.debug('[analytics] pageView', path, context),
  track: (event, context) => console.debug('[analytics] track', event.name, event, context),
  identify: (user) => console.debug('[analytics] identify', user),
});
