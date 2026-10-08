import { setupWorker } from 'msw/browser';
import { handlers } from '@/api/mock/handlers';
import { createFeedHandlers } from '@/mocks/handlers';

export const worker = setupWorker(
  ...createFeedHandlers({ includeProfile: false, profileCases: true }),
  ...handlers,
);
