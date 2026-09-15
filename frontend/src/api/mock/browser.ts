import { setupWorker } from 'msw/browser';
import { handlers } from '@/api/mock/handlers';

export const worker = setupWorker(...handlers);
