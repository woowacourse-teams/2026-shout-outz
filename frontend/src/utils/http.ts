import ky from 'ky';
import { getApiOrigin } from '@/utils/auth';

let csrfToken: string | null = null;

export const setCsrfToken = (token: string | null) => {
  csrfToken = token;
};

const CSRF_HEADER = 'X-CSRF-TOKEN';
const MUTATION_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

const apiOrigin = getApiOrigin();

export const kyInstance = ky.create({
  baseUrl: apiOrigin,
  timeout: 10_000,
  totalTimeout: 30_000,
  credentials: 'include',
  retry: {
    limit: 2,
    methods: ['get'],
  },

  hooks: {
    beforeRequest: [
      ({ request }) => {
        if (!MUTATION_METHODS.has(request.method)) return;
        if (csrfToken) {
          request.headers.set(CSRF_HEADER, csrfToken);
        }
      },
    ],
  },
});
