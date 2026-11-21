/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { QueryClient } from '@tanstack/react-query';
import { screen } from '@testing-library/react';

import { exploreSsrQueries, type ExploreSearch } from '@/routes/index';
import { renderRoute, server } from '@/test/renderRoute';
import { ensureSsrQueries } from '@/utils/ssr-queries';

const requestedPaths = () => {
  const paths = new Set<string>();
  const listener = ({ request }: { request: Request }) => {
    const url = new URL(request.url);
    // 쿼리 키가 다르면 같은 API를 다른 쿼리 문자열로 부르므로, 경로만 비교한다.
    paths.add(url.pathname);
  };
  server.events.on('request:start', listener);
  return { paths, stop: () => server.events.removeListener('request:start', listener) };
};

// SSR 서버(server/render.tsx)처럼 ssrQueries를 미리 받아 둔 캐시를 만든다.
// 브라우저에서는 하이드레이션 때 서버 데이터를 잠시 새 데이터로 보므로(ssg/router.ts) staleTime을 넉넉히 둔다.
async function prefetchLikeServer(search: ExploreSearch) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity } },
  });
  const recording = requestedPaths();
  await ensureSsrQueries(queryClient, exploreSsrQueries(search));
  recording.stop();
  return { queryClient, prefetched: recording.paths };
}

describe('홈 서버 렌더 전에 받아 두는 쿼리(exploreSsrQueries)', () => {
  it.each<[string, ExploreSearch, RegExp]>([
    ['/', {}, /^이런 컨텐츠는 어때요/],
    ['/?tab=job', { tab: 'job' }, /^채용공고/],
    ['/?tab=job&sort=DEADLINE', { tab: 'job', sort: 'DEADLINE' }, /^채용공고/],
    ['/?tab=project', { tab: 'project' }, /프로젝트/],
    ['/?tab=project&sort=POPULAR', { tab: 'project', sort: 'POPULAR' }, /프로젝트/],
  ])('%s: 미리 받은 API를 화면이 다시 부르지 않는다', async (entry, search, heading) => {
    const { queryClient, prefetched } = await prefetchLikeServer(search);
    expect(prefetched.size).toBeGreaterThan(0);

    const recording = requestedPaths();
    renderRoute(entry, { queryClient });
    expect(
      (await screen.findAllByRole('heading', { level: 2, name: heading })).length,
    ).toBeGreaterThan(0);
    recording.stop();

    expect([...recording.paths].filter((path) => prefetched.has(path))).toEqual([]);
  });

  it('검색어나 기술 주제로 거른 화면은 미리 받지 않고 스트리밍으로 이어 보낸다', () => {
    expect(exploreSsrQueries({ q: 'LLM' })).toEqual([]);
    expect(exploreSsrQueries({ tab: 'job', q: '토스' })).toEqual([]);
    expect(exploreSsrQueries({ tab: 'project', topic: 'JPA/Hibernate' })).toEqual([]);
  });
});
