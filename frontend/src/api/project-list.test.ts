/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { http, HttpResponse } from 'msw';

import { fetchProjectList } from '@/api/project-list';
import { PROJECT_PAGE_SIZE } from '@/constants/project';
import { server } from '@/test/renderRoute';

/** 커서마다 다른 페이지를 돌려주고, 받은 쿼리를 기록한다. */
const mockPages = () => {
  const requests: URLSearchParams[] = [];
  const pages: Record<string, { slugs: string[]; nextCursor: string | null }> = {
    '': { slugs: ['a', 'b'], nextCursor: 'c1' },
    c1: { slugs: ['c', 'd'], nextCursor: 'c2' },
    c2: { slugs: ['e'], nextCursor: null },
  };

  server.use(
    http.get('/api/v1/projects', ({ request }) => {
      const params = new URL(request.url).searchParams;
      requests.push(params);
      const page = pages[params.get('cursor') ?? '']!;
      return HttpResponse.json({
        status: 'success',
        data: page.slugs.map((slug) => ({ slug })),
        meta: { nextCursor: page.nextCursor, hasNext: page.nextCursor !== null, totalCount: 5 },
      });
    }),
  );

  return requests;
};

describe('fetchProjectList', () => {
  it('nextCursor를 따라 마지막 페이지까지 모두 받는다', async () => {
    const requests = mockPages();

    const projects = await fetchProjectList();

    expect(projects.map(({ slug }) => slug)).toEqual(['a', 'b', 'c', 'd', 'e']);
    expect(requests.map((params) => params.get('cursor'))).toEqual([null, 'c1', 'c2']);
    expect(requests.every((params) => params.get('size') === String(PROJECT_PAGE_SIZE))).toBe(true);
  });
});
