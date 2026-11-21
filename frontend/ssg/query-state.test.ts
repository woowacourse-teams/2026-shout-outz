import { QueryClient } from '@tanstack/react-query';
import { hydrateServerQueries, toQueryStateScript } from './query-state';

const parseScript = (script: string) => {
  const match =
    /^<script>self\.__SSR_QUERY_STATE__=(.*);document\.currentScript\.remove\(\)<\/script>$/.exec(
      script,
    );
  if (!match) throw new Error(`스크립트 모양이 다릅니다: ${script}`);
  return JSON.parse(match[1]!);
};

describe('toQueryStateScript', () => {
  it('서버 렌더 중 받은 쿼리를 담고, 이미 $_TSR에 담긴 쿼리는 뺀다', async () => {
    const queryClient = new QueryClient();
    await queryClient.fetchQuery({ queryKey: ['feed', 1], queryFn: () => ({ id: 1 }) });
    const inRouterState = new Set(
      queryClient
        .getQueryCache()
        .getAll()
        .map((q) => q.queryHash),
    );
    await queryClient.fetchQuery({ queryKey: ['explore', 'recent'], queryFn: () => ['블로그'] });

    const state = parseScript(toQueryStateScript(queryClient, inRouterState));

    expect(state.queries.map((q: { queryKey: unknown }) => q.queryKey)).toEqual([
      ['explore', 'recent'],
    ]);
    expect(state.queries[0].state.data).toEqual(['블로그']);
  });

  it('실패한 쿼리는 담지 않고, 담을 것이 없으면 빈 문자열', async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    await queryClient
      .fetchQuery({
        queryKey: ['broken'],
        queryFn: () => Promise.reject(new Error('x')),
      })
      .catch(() => {});

    expect(toQueryStateScript(queryClient, new Set())).toBe('');
  });

  it('데이터에 </script>나 줄 구분 문자가 있어도 스크립트가 끊기지 않는다', async () => {
    const queryClient = new QueryClient();
    const text = '</script><script>alert(1)</script> 끝';
    await queryClient.fetchQuery({ queryKey: ['xss'], queryFn: () => text });

    const script = toQueryStateScript(queryClient, new Set());

    expect(script.match(/<\/script>/g)).toHaveLength(1);
    expect(script).not.toContain(' ');
    expect(parseScript(script).queries[0].state.data).toBe(text);
  });
});

describe('hydrateServerQueries', () => {
  it('넘겨받은 쿼리를 캐시에 넣고, 하이드레이션 직후 다시 받지 않게 staleTime을 둔다', async () => {
    const server = new QueryClient();
    await server.fetchQuery({ queryKey: ['explore', 'recent'], queryFn: () => ['블로그'] });
    const state = parseScript(toQueryStateScript(server, new Set()));

    const browser = new QueryClient();
    hydrateServerQueries(browser, state);

    expect(browser.getQueryData(['explore', 'recent'])).toEqual(['블로그']);
    expect(browser.getQueryDefaults(['explore', 'recent']).staleTime).toBe(60_000);
  });

  it('넘겨받은 것이 없으면 아무것도 하지 않는다', () => {
    const browser = new QueryClient();
    hydrateServerQueries(browser, undefined);
    expect(browser.getQueryCache().getAll()).toHaveLength(0);
  });
});
