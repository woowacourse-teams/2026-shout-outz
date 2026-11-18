/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { http, HttpResponse } from 'msw';

import { renderRoute, server } from '@/test/renderRoute';

const resultSection = async (name: RegExp) => screen.findByRole('region', { name });

const serverMaterial = (resourceId: number, source: string, title: string) => ({
  resourceId,
  source,
  title,
  url: `https://github.com/woowacourse-teams/sample/${resourceId}`,
  summary: `${title} 한줄 소개`,
  publishedAt: '2026-10-20T05:12:00Z',
  platform: null,
  team: {
    name: '샤라웃',
    projectSlug: 'shout-outz',
    logoMediaId: 31,
    logoUrl: 'https://cdn.example.com/media/31/thumbnail.webp',
  },
});

describe('ExplorePage', () => {
  it('홈(/)이 탐색 화면이고, 헤더에서 탐색을 현재 페이지로 표시한다', async () => {
    renderRoute('/');

    expect(await screen.findByRole('heading', { name: '이런 컨텐츠는 어때요?' })).toBeVisible();

    const header = await screen.findByRole('banner');
    expect(within(header).getByRole('link', { name: '탐색' })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  describe('<head>', () => {
    const meta = (selector: string) => document.querySelector(selector)?.getAttribute('content');
    const canonical = () => document.querySelector('link[rel="canonical"]')?.getAttribute('href');

    it('홈 첫 화면은 서비스 제목·설명과 canonical(/)을 넣는다', async () => {
      renderRoute('/');

      await waitFor(() =>
        expect(document.title).toBe('shout-outz | 우테코 크루들의 프로젝트 경험과 취업 과정'),
      );
      expect(meta('meta[name="description"]')).toMatch(
        /^우테코 크루들의 프로젝트 경험과 취업 과정/,
      );
      expect(meta('meta[property="og:title"]')).toBe(document.title);
      expect(canonical()).toBe('https://shout-ou.tz/');
      expect(document.querySelector('meta[name="robots"]')).toBeNull();
      const jsonLd = document.querySelector('script[type="application/ld+json"]');
      expect(JSON.parse(jsonLd?.textContent ?? '{}')).toMatchObject({
        '@type': 'WebSite',
        name: 'shout-outz',
        url: 'https://shout-ou.tz/',
      });
    });

    it('채용 공고 탭은 탭 주소를 canonical로 둔다', async () => {
      renderRoute('/?tab=job');

      await waitFor(() => expect(document.title).toBe('채용 공고 | shout-outz'));
      expect(canonical()).toBe('https://shout-ou.tz/?tab=job');
      expect(document.querySelector('script[type="application/ld+json"]')).toBeNull();
    });

    it('검색 결과는 색인하지 않는다', async () => {
      renderRoute('/?q=동시성');

      await waitFor(() => expect(document.title).toBe('동시성 검색 결과 | shout-outz'));
      expect(meta('meta[name="robots"]')).toBe('noindex');
      expect(canonical()).toBeUndefined();
    });
  });

  it('예전 /explore 주소로 들어오면 검색어와 탭을 그대로 들고 홈으로 간다', async () => {
    const router = renderRoute('/explore?tab=job&q=토스');

    await waitFor(() => expect(router.state.location.pathname).toBe('/'));
    expect(router.state.location.search).toEqual({ tab: 'job', q: '토스' });
  });

  it('검색 전에는 추천 콘텐츠와 키워드 목록을 보여준다', async () => {
    renderRoute('/');

    expect(await screen.findByRole('heading', { name: '이런 컨텐츠는 어때요?' })).toBeVisible();
    expect(screen.getByRole('heading', { name: '많이 다룬 키워드' })).toBeVisible();
    expect(screen.getByRole('heading', { name: '분야별 키워드' })).toBeVisible();
    expect(screen.getByText('전체 77개 주제를 분야별로 모았어요.')).toBeVisible();
  });

  it('처음에는 키워드 탭을 고르고, 모든 탭을 누를 수 있다', async () => {
    renderRoute('/');

    const tabs = await screen.findByRole('tablist', { name: '찾는 방법' });
    expect(within(tabs).getByRole('tab', { name: '키워드' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    expect(within(tabs).getByRole('tab', { name: '채용공고' })).toBeEnabled();
    expect(within(tabs).getByRole('tab', { name: '프로젝트' })).toBeEnabled();
  });

  it('검색어를 입력하면 그 검색어로 찾은 자료를 종류별로 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/');

    await user.type(await screen.findByRole('searchbox', { name: '기술 이야기 검색' }), '동시성');
    await user.click(screen.getByRole('button', { name: '검색' }));

    const summary = await screen.findByRole('region', { name: '검색 요약' });
    expect(summary).toHaveTextContent('동시성 검색 결과');
    expect(await resultSection(/^블로그/)).toBeVisible();
    expect(await resultSection(/^테코톡/)).toBeVisible();
    expect(await resultSection(/^PR과 팀 문서/)).toBeVisible();
  });

  it('추천 검색어를 누르면 그 검색어로 검색한다', async () => {
    const user = userEvent.setup();
    renderRoute('/');

    const suggestions = await screen.findByRole('list', { name: '추천 검색어' });
    await user.click(within(suggestions).getByRole('button', { name: '무중단 배포' }));

    expect(await screen.findByRole('searchbox', { name: '기술 이야기 검색' })).toHaveValue(
      '무중단 배포',
    );
    expect(await resultSection(/^블로그/)).toBeVisible();
    // 검색한 뒤에는 추천 검색어를 숨긴다.
    expect(screen.queryByRole('list', { name: '추천 검색어' })).not.toBeInTheDocument();
  });

  it('검색어를 지우고 검색하면 검색을 풀고 추천 검색어를 다시 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/?q=LLM');

    const searchbox = await screen.findByRole('searchbox', { name: '기술 이야기 검색' });
    expect(screen.queryByRole('list', { name: '추천 검색어' })).not.toBeInTheDocument();

    await user.clear(searchbox);
    await user.click(screen.getByRole('button', { name: '검색' }));

    expect(await screen.findByRole('list', { name: '추천 검색어' })).toBeVisible();
  });

  it('자료 종류 탭을 고르면 그 종류만 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/?q=동시성');

    const kinds = await screen.findByRole('tablist', { name: '자료 종류' });
    await user.click(within(kinds).getByRole('tab', { name: /테코톡/ }));

    expect(await resultSection(/^테코톡/)).toBeVisible();
    expect(screen.queryByRole('region', { name: /^블로그/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('region', { name: /^PR과 팀 문서/ })).not.toBeInTheDocument();
  });

  it('위키와 디스커션 자료는 팀 문서 배지와 팀 로고로 보여준다', async () => {
    server.use(
      http.get('*/api/v1/resources/search', () =>
        HttpResponse.json({
          status: 'success',
          data: {
            blogs: { items: [] },
            documents: {
              items: [
                serverMaterial(1, 'github-wiki', '위키 자료'),
                serverMaterial(2, 'github-discussion', '디스커션 자료'),
              ],
            },
            talks: { items: [] },
          },
        }),
      ),
    );
    renderRoute('/?q=동시성');

    const documents = await resultSection(/^PR과 팀 문서/);
    for (const title of ['위키 자료', '디스커션 자료']) {
      const item = within(documents).getByRole('button', { name: new RegExp(title) });
      expect(within(item).getByText('팀 문서')).toBeVisible();
      // 팀 로고는 서버가 준 logoUrl 이미지로 보여 준다.
      expect(
        item.querySelector('img[src="https://cdn.example.com/media/31/thumbnail.webp"]'),
      ).toBeInTheDocument();
      expect(within(item).getByRole('img', { name: 'GitHub 팀 문서' })).toBeVisible();
    }
  });

  it('블로그 아이콘은 서버가 준 faviconUrl로 보여 주고, 없거나 열리지 않으면 숨긴다', async () => {
    const blog = (resourceId: number, title: string, faviconUrl: string | null) => ({
      ...serverMaterial(resourceId, 'blog-post', title),
      team: { name: '샤라웃', projectSlug: 'shout-outz', logoMediaId: null, logoUrl: null },
      faviconUrl,
    });
    server.use(
      http.get('*/api/v1/resources/search', () =>
        HttpResponse.json({
          status: 'success',
          data: {
            blogs: {
              items: [
                blog(11, '파비콘 있는 글', 'https://blog.example.com/favicon.png'),
                blog(12, '파비콘 없는 글', null),
                blog(13, '파비콘이 깨진 글', 'https://broken.example.com/favicon.ico'),
              ],
            },
            documents: { items: [] },
            talks: { items: [] },
          },
        }),
      ),
    );
    renderRoute('/?q=동시성');

    const blogs = await resultSection(/^블로그/);
    const itemOf = (title: string) =>
      within(blogs).getByRole('button', { name: new RegExp(title) });

    expect(
      itemOf('파비콘 있는 글').querySelector('img[src="https://blog.example.com/favicon.png"]'),
    ).toBeInTheDocument();
    expect(itemOf('파비콘 없는 글').querySelector('img')).not.toBeInTheDocument();

    const broken = itemOf('파비콘이 깨진 글').querySelector('img');
    if (!broken) throw new Error('깨진 파비콘 이미지를 찾을 수 없습니다.');
    fireEvent.error(broken);
    await waitFor(() =>
      expect(itemOf('파비콘이 깨진 글').querySelector('img')).not.toBeInTheDocument(),
    );
  });

  it('로고를 등록하지 않은 팀은 로고 자리 없이 팀 이름만 보여준다', async () => {
    const material = serverMaterial(3, 'github-pr', '로고 없는 팀 PR');
    server.use(
      http.get('*/api/v1/resources/search', () =>
        HttpResponse.json({
          status: 'success',
          data: {
            blogs: { items: [] },
            documents: {
              items: [
                { ...material, team: { ...material.team, logoMediaId: null, logoUrl: null } },
              ],
            },
            talks: { items: [] },
          },
        }),
      ),
    );
    renderRoute('/?q=동시성');

    const documents = await resultSection(/^PR과 팀 문서/);
    const item = within(documents).getByRole('button', { name: /로고 없는 팀 PR/ });
    expect(within(item).getByText('샤라웃')).toBeVisible();
    // 이미지도, 회색 칸도 그리지 않는다.
    expect(item.querySelector('img')).not.toBeInTheDocument();
    expect(item.querySelector('.bg-gray-200')).not.toBeInTheDocument();
  });

  it('자료 요약에서 서버가 원인과 결과를 주지 않은 줄은 뺀다', async () => {
    const user = userEvent.setup();
    server.use(
      http.get('*/api/v1/resources/search', () =>
        HttpResponse.json({
          status: 'success',
          data: {
            blogs: { items: [] },
            documents: { items: [serverMaterial(7, 'github-pr', '캐시 복구 PR')] },
            talks: { items: [] },
          },
        }),
      ),
      http.get('*/api/v1/resources/7', () =>
        HttpResponse.json({
          status: 'success',
          data: {
            ...serverMaterial(7, 'github-pr', '캐시 복구 PR'),
            contentType: 'problem_solving',
            techTags: ['oEmbed'],
            keyPoints: ['봇 확인 페이지가 캐시되는 원인을 알아봐요.'],
            experiences: [
              {
                problem: '미리보기가 깨졌다.',
                cause: null,
                solution: 'oEmbed를 먼저 썼다.',
                result: null,
                techs: [],
              },
            ],
            linkPreview: { title: null, description: null, imageUrl: null, siteName: null },
            topic: { topicId: 'cache', name: '캐시' },
          },
        }),
      ),
    );
    renderRoute('/?q=캐시');

    const documents = await resultSection(/^PR과 팀 문서/);
    await user.click(within(documents).getByRole('button', { name: /캐시 복구 PR/ }));
    const summary = await screen.findByRole('dialog');

    expect(
      await within(summary).findByText('봇 확인 페이지가 캐시되는 원인을 알아봐요.'),
    ).toBeVisible();
    expect(within(summary).getByText('미리보기가 깨졌다.')).toBeVisible();
    expect(within(summary).getByText('oEmbed를 먼저 썼다.')).toBeVisible();
    expect(within(summary).queryByText('원인')).not.toBeInTheDocument();
    expect(within(summary).queryByText('결과')).not.toBeInTheDocument();
  });

  it.each([
    ['링크 미리보기가 아직 없으면', null],
    ['대표 이미지가 열리지 않으면(429 등)', 'https://opengraph.githubassets.com/x/sample/8'],
  ])(
    '원문 카드는 %s 원문 사이트 파비콘, 그것도 안 열리면 회색 칸을 보여준다',
    async (_, imageUrl) => {
      const user = userEvent.setup();
      server.use(
        http.get('*/api/v1/resources/search', () =>
          HttpResponse.json({
            status: 'success',
            data: {
              blogs: { items: [] },
              documents: { items: [serverMaterial(8, 'github-pr', '미리보기 없는 PR')] },
              talks: { items: [] },
            },
          }),
        ),
        http.get('*/api/v1/resources/8', () =>
          HttpResponse.json({
            status: 'success',
            data: {
              ...serverMaterial(8, 'github-pr', '미리보기 없는 PR'),
              contentType: null,
              techTags: [],
              keyPoints: [],
              experiences: [],
              linkPreview: imageUrl
                ? { title: null, description: null, imageUrl, siteName: null }
                : null,
              topic: { topicId: 'cache', name: '캐시' },
            },
          }),
        ),
      );
      renderRoute('/?q=미리보기');

      const documents = await resultSection(/^PR과 팀 문서/);
      await user.click(within(documents).getByRole('button', { name: /미리보기 없는 PR/ }));
      const card = within(await screen.findByRole('dialog')).getByRole('link', {
        name: /^원문 보기:/,
      });
      const thumbnail = () => card.querySelector('img');

      if (imageUrl) {
        await waitFor(() => expect(thumbnail()).toHaveAttribute('src', imageUrl));
        fireEvent.error(thumbnail()!);
      }
      await waitFor(() =>
        expect(thumbnail()).toHaveAttribute('src', 'https://github.com/favicon.ico'),
      );

      fireEvent.error(thumbnail()!);
      expect(thumbnail()).not.toBeInTheDocument();
      expect(card.querySelector('.bg-gray-100')).toBeInTheDocument();
    },
  );

  it('찾은 자료가 하나도 없으면 안내를 보여준다', async () => {
    renderRoute('/?q=없는검색어123');

    expect(
      await screen.findByText("'없는검색어123'에 맞는 자료를 아직 못 찾았어요."),
    ).toBeVisible();
  });

  it('테코톡은 영상 링크를 새 탭으로 연다', async () => {
    renderRoute('/?q=동시성');

    const talks = await resultSection(/^테코톡/);
    const [firstTalk] = within(talks).getAllByRole('link');
    expect(firstTalk).toHaveAttribute('href', expect.stringContaining('youtu'));
    expect(firstTalk).toHaveAttribute('target', '_blank');
  });

  it('PR을 누르면 기술 태그와 문제 해결 경험이 있는 요약을 연다', async () => {
    const user = userEvent.setup();
    renderRoute('/?q=동시성');

    const documents = await resultSection(/^PR과 팀 문서/);
    await user.click(within(documents).getAllByRole('button', { name: /^PR/ })[0]!);

    const summary = await screen.findByRole('dialog');
    expect(within(summary).getByRole('link', { name: /^원문 보기:/ })).toHaveAttribute(
      'target',
      '_blank',
    );
    expect(within(summary).getByRole('link', { name: '상세 보기' })).toHaveAttribute(
      'href',
      expect.stringMatching(/^\/materials\//),
    );
    expect(await within(summary).findByRole('heading', { name: '기술 태그' })).toBeVisible();
    expect(within(summary).getByRole('heading', { name: '이 자료에서 배울 점' })).toBeVisible();
    expect(
      within(summary).getByRole('heading', { name: /어떤 문제를 어떻게 풀었나/ }),
    ).toBeVisible();

    // jsdom에는 vaul 바텀시트가 쓰는 포인터 캡처가 없어 닫기 버튼 대신 Esc로 닫는다.
    expect(within(summary).getByRole('button', { name: '닫기' })).toBeVisible();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('요약 아래 상세 보기를 누르면 모달을 닫고 자료 정리 보기 페이지로 간다', async () => {
    const user = userEvent.setup();
    const router = renderRoute('/?q=동시성');

    const documents = await resultSection(/^PR과 팀 문서/);
    await user.click(within(documents).getAllByRole('button', { name: /^PR/ })[0]!);
    const summary = await screen.findByRole('dialog');
    // jsdom에는 vaul 바텀시트가 쓰는 포인터 캡처가 없어 클릭 대신 Enter로 누른다.
    within(summary).getByRole('link', { name: '상세 보기' }).focus();
    await user.keyboard('{Enter}');

    await waitFor(() => expect(router.state.location.pathname).toMatch(/^\/materials\//));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await screen.findByRole('heading', { level: 1 })).toBeVisible();
    expect(screen.getByRole('link', { name: /^원문 보기:/ })).toHaveAttribute('target', '_blank');
    expect(await screen.findByRole('heading', { name: '기술 태그' })).toBeVisible();
  });

  it('블로그 요약에는 기술 태그와 문제 해결 경험이 없다', async () => {
    const user = userEvent.setup();
    renderRoute('/?q=동시성');

    const blogs = await resultSection(/^블로그/);
    await user.click(within(blogs).getAllByRole('button', { name: /^블로그/ })[0]!);

    const summary = await screen.findByRole('dialog');
    expect(
      await within(summary).findByRole('heading', { name: '이 자료에서 배울 점' }),
    ).toBeVisible();
    expect(within(summary).queryByRole('heading', { name: '기술 태그' })).not.toBeInTheDocument();
    expect(
      within(summary).queryByRole('heading', { name: /어떤 문제를 어떻게 풀었나/ }),
    ).not.toBeInTheDocument();
  });
});

describe('ExplorePage 검색 결과 자료 종류와 이어 읽기', () => {
  const prs = (from: number, count: number) =>
    Array.from({ length: count }, (_, index) =>
      serverMaterial(from + index, 'github-pr', `PR ${from + index}`),
    );
  const section = (items: unknown[], nextCursor: string | null = null) => ({
    items,
    nextCursor,
    hasNext: nextCursor !== null,
  });
  const searchResponse = (data: Record<string, unknown>) =>
    http.get('*/api/v1/resources/search', () =>
      HttpResponse.json({
        status: 'success',
        data: {
          blogs: section([]),
          documents: section([]),
          talks: section([]),
          matchedProjects: [],
          ...data,
        },
      }),
    );
  const prCount = (region: HTMLElement) =>
    within(region).queryAllByRole('button', { name: /PR \d+/ }).length;

  it('주소의 자료 종류로 들어오면 그 종류만 보여 주고, 칩에는 건수를 쓰지 않는다', async () => {
    renderRoute('/?q=동시성&kind=TALK');

    const kinds = await screen.findByRole('tablist', { name: '자료 종류' });
    expect(within(kinds).getByRole('tab', { name: '테코톡' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    expect(await resultSection(/^테코톡$/)).toBeVisible();
    expect(screen.queryByRole('region', { name: /^블로그/ })).not.toBeInTheDocument();
  });

  it('자료 종류를 고르면 주소에 남기고, 검색어를 바꾸면 전체로 되돌린다', async () => {
    const user = userEvent.setup();
    const router = renderRoute('/?q=동시성');

    const kinds = await screen.findByRole('tablist', { name: '자료 종류' });
    await user.click(within(kinds).getByRole('tab', { name: '블로그' }));
    await waitFor(() =>
      expect(router.state.location.search).toEqual({ q: '동시성', kind: 'BLOG' }),
    );

    const searchbox = screen.getByRole('searchbox', { name: '기술 이야기 검색' });
    await user.clear(searchbox);
    await user.type(searchbox, 'LLM');
    await user.click(screen.getByRole('button', { name: '검색' }));
    await waitFor(() => expect(router.state.location.search).toEqual({ q: 'LLM' }));
  });

  it('자료가 없는 종류는 칩을 두지 않고, 한 종류만 남으면 전체 칩도 두지 않는다', async () => {
    server.use(searchResponse({ blogs: section(prs(1, 1)), documents: section(prs(2, 1)) }));
    renderRoute('/?q=동시성');

    const kinds = await screen.findByRole('tablist', { name: '자료 종류' });
    expect(
      within(kinds)
        .getAllByRole('tab')
        .map((tab) => tab.textContent),
    ).toEqual(['전체', '블로그', 'PR과 팀 문서']);
  });

  it('한 종류만 있으면 칩 없이 그 종류를 모두 보여 준다', async () => {
    server.use(searchResponse({ documents: section(prs(1, 12)) }));
    renderRoute('/?q=동시성');

    const documents = await resultSection(/^PR과 팀 문서$/);
    expect(prCount(documents)).toBe(12);
    expect(screen.queryByRole('tablist', { name: '자료 종류' })).not.toBeInTheDocument();
  });

  it('모두 보기로 그 종류만 보고, 더 보기는 받은 커서로 이어 읽다가 커서가 없으면 숨긴다', async () => {
    const requests: Record<string, string | null>[] = [];
    server.use(
      searchResponse({ blogs: section(prs(100, 1)), documents: section(prs(1, 20), 'cursor-1') }),
      http.get('*/api/v1/resources/search/more', ({ request }) => {
        const params = new URL(request.url).searchParams;
        requests.push({
          keyword: params.get('keyword'),
          section: params.get('section'),
          cursor: params.get('cursor'),
          track: params.get('track'),
        });
        // 받는 건수는 20건보다 적을 수 있고, 더 있는지는 hasNext로만 정한다.
        const page =
          params.get('cursor') === 'cursor-1'
            ? { data: prs(21, 3), meta: { nextCursor: 'cursor-2', hasNext: true, totalCount: 24 } }
            : { data: prs(24, 1), meta: { nextCursor: null, hasNext: false, totalCount: 24 } };
        return HttpResponse.json({ status: 'success', ...page });
      }),
    );
    const user = userEvent.setup();
    const router = renderRoute('/?q=동시성');

    // 전체에서는 앞의 5건만 보여 준다.
    expect(prCount(await resultSection(/^PR과 팀 문서$/))).toBe(5);
    await user.click(screen.getByRole('button', { name: 'PR과 팀 문서 모두 보기' }));

    await waitFor(() =>
      expect(router.state.location.search).toEqual({ q: '동시성', kind: 'DOCUMENT' }),
    );
    const documents = await resultSection(/^PR과 팀 문서$/);
    expect(prCount(documents)).toBe(20);
    expect(screen.queryByRole('region', { name: /^블로그/ })).not.toBeInTheDocument();
    // 칩만 바꾸고 이어 읽기는 아직 부르지 않는다.
    expect(requests).toEqual([]);

    await user.click(within(documents).getByRole('button', { name: 'PR과 팀 문서 더 보기' }));
    await waitFor(() => expect(prCount(documents)).toBe(23));
    await user.click(within(documents).getByRole('button', { name: 'PR과 팀 문서 더 보기' }));
    await waitFor(() => expect(prCount(documents)).toBe(24));

    expect(
      within(documents).queryByRole('button', { name: 'PR과 팀 문서 더 보기' }),
    ).not.toBeInTheDocument();
    expect(requests).toEqual([
      { keyword: '동시성', section: 'DOCUMENT', cursor: 'cursor-1', track: null },
      { keyword: '동시성', section: 'DOCUMENT', cursor: 'cursor-2', track: null },
    ]);
  });
});

describe('ExplorePage 채용공고 탭', () => {
  it('채용공고 탭을 누르면 공고 카드 목록을 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/');

    const tabs = await screen.findByRole('tablist', { name: '찾는 방법' });
    await user.click(within(tabs).getByRole('tab', { name: '채용공고' }));

    const cards = await screen.findByRole('list', { name: '공고' });
    expect(
      within(cards).getByRole('link', { name: /Server Developer \(Product\)/ }),
    ).toHaveAttribute('href', '/job-postings/1');
    expect(within(cards).getByRole('link', { name: /Frontend Developer/ })).toBeVisible();
    expect(within(cards).getByText('곧 추가될 공고예요')).toBeInTheDocument();
    expect(within(cards).getAllByText('상시')).toHaveLength(2);
    expect(within(cards).getByText('D-3')).toBeVisible();
    expect(within(cards).getByText('D-day')).toBeVisible();
    // 회사 로고와 해시태그는 서버 응답에 없어 아직 보여 주지 않는다.
    expect(within(cards).queryByRole('img', { name: /로고$/ })).not.toBeInTheDocument();
    expect(screen.getByRole('searchbox', { name: '채용공고 검색' })).toBeVisible();
  });

  it('추천 검색어로 공고를 찾으면 맞는 공고만 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/?tab=job');

    const suggestions = await screen.findByRole('list', { name: '추천 검색어' });
    await user.click(within(suggestions).getByRole('button', { name: '토스증권' }));

    const cards = await screen.findByRole('list', { name: '공고' });
    expect(await within(cards).findAllByRole('link')).toHaveLength(1);
    expect(within(cards).getByRole('link', { name: /Frontend Developer/ })).toBeVisible();
    expect(within(cards).queryByText('곧 추가될 공고예요')).not.toBeInTheDocument();
  });

  it('맞는 공고가 없으면 안내를 보여준다', async () => {
    renderRoute('/?tab=job&q=없는회사123');

    expect(await screen.findByText("'없는회사123'에 맞는 공고를 아직 못 찾았어요.")).toBeVisible();
  });
});

describe('ExplorePage 프로젝트 탭', () => {
  it('프로젝트 탭을 누르면 프로젝트 카드 목록과 기술 주제를 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/');

    const tabs = await screen.findByRole('tablist', { name: '찾는 방법' });
    await user.click(within(tabs).getByRole('tab', { name: '프로젝트' }));

    const cards = await screen.findByRole('list', { name: '프로젝트' });
    expect(within(cards).getByRole('link', { name: /Shout-outz/ })).toHaveAttribute(
      'href',
      '/projects/@shout-outz',
    );
    expect(screen.getByRole('heading', { name: /^프로젝트 \d+$/ })).toBeVisible();
    expect(screen.getByRole('searchbox', { name: '프로젝트 검색' })).toBeVisible();

    const nav = await screen.findByRole('navigation', { name: '기술 주제' });
    expect(within(nav).getByRole('button', { name: '전체' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });

  it('목록 아래에서 우리 팀 프로젝트 등록으로 이어진다', async () => {
    renderRoute('/?tab=project');

    await screen.findByRole('list', { name: '프로젝트' });
    expect(screen.getByText(/찾는 프로젝트가 없나요\?/)).toBeVisible();
    expect(screen.getByRole('link', { name: '우리 팀 프로젝트 등록하기 →' })).toHaveAttribute(
      'href',
      '/projects/new',
    );
  });

  it('기술 주제를 고르면 그 주제를 다룬 프로젝트만 보여주고, 다시 누르면 푼다', async () => {
    const user = userEvent.setup();
    renderRoute('/?tab=project');

    const nav = await screen.findByRole('navigation', { name: '기술 주제' });
    await user.click(within(nav).getByRole('button', { name: 'BE' }));
    const topic = within(nav).getByRole('button', { name: /트랜잭션\/데드락/ });
    await user.click(topic);

    expect(await screen.findByRole('heading', { name: '프로젝트 1' })).toBeVisible();
    const cards = screen.getByRole('list', { name: '프로젝트' });
    expect(within(cards).getByRole('link', { name: /Shout-outz/ })).toBeVisible();
    expect(within(cards).queryByRole('link', { name: /Dropit/ })).not.toBeInTheDocument();
    expect(topic).toHaveAttribute('aria-pressed', 'true');
    // 주제를 고르면 그 주제로 검색한 것으로 보고 검색창에 넣고 추천 검색어를 숨긴다.
    expect(screen.getByRole('searchbox', { name: '프로젝트 검색' })).toHaveValue('트랜잭션/데드락');
    expect(screen.queryByRole('list', { name: '추천 검색어' })).not.toBeInTheDocument();

    await user.click(topic);
    expect(await screen.findByRole('heading', { name: '프로젝트 13' })).toBeVisible();
    expect(screen.getByRole('searchbox', { name: '프로젝트 검색' })).toHaveValue('');
    expect(screen.getByRole('list', { name: '추천 검색어' })).toBeVisible();
  });

  it('검색어와 기술 주제는 마지막에 고른 것 하나로만 거른다', async () => {
    const user = userEvent.setup();
    renderRoute('/?tab=project&q=Dropit');

    expect(await screen.findByText('키워드로 찾은 결과', { exact: false })).toHaveTextContent(
      'Dropit 키워드로 찾은 결과',
    );

    const nav = await screen.findByRole('navigation', { name: '기술 주제' });
    await user.click(within(nav).getByRole('button', { name: 'BE' }));
    await user.click(within(nav).getByRole('button', { name: /트랜잭션\/데드락/ }));
    await waitFor(() =>
      expect(screen.getByText('키워드로 찾은 결과', { exact: false })).toHaveTextContent(
        '#트랜잭션/데드락 키워드로 찾은 결과',
      ),
    );
    const searchbox = screen.getByRole('searchbox', { name: '프로젝트 검색' });
    expect(searchbox).toHaveValue('트랜잭션/데드락');

    await user.clear(searchbox);
    await user.type(searchbox, 'Knot');
    await user.click(screen.getByRole('button', { name: '검색' }));
    await waitFor(() =>
      expect(screen.getByText('키워드로 찾은 결과', { exact: false })).toHaveTextContent(
        'Knot 키워드로 찾은 결과',
      ),
    );
    expect(within(nav).getByRole('button', { name: /트랜잭션\/데드락/ })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
  });

  it('추천 주제를 누르면 기술 주제로 거른다', async () => {
    const user = userEvent.setup();
    renderRoute('/?tab=project');

    const suggestions = await screen.findByRole('list', { name: '추천 검색어' });
    await user.click(within(suggestions).getByRole('button', { name: '트랜잭션/데드락' }));

    expect(await screen.findByRole('heading', { name: '프로젝트 1' })).toBeVisible();
    expect(screen.getByRole('searchbox', { name: '프로젝트 검색' })).toHaveValue('트랜잭션/데드락');
    expect(screen.queryByRole('list', { name: '추천 검색어' })).not.toBeInTheDocument();
  });

  describe('거른 프로젝트 카드', () => {
    const projectList = (meta: object, item: object = {}) =>
      http.get('/api/v1/projects', () =>
        HttpResponse.json({
          status: 'success',
          data: [
            {
              slug: 'dropit',
              title: 'Dropit',
              tagline: '드롭 서비스',
              cohort: 8,
              thumbnailUrl: null,
              likeCount: 3,
              likedByMe: false,
              commentCount: 0,
              bookmarkCount: 0,
              bookmarkedByMe: false,
              techTags: [],
              members: [],
              ...item,
            },
          ],
          meta: { nextCursor: null, hasNext: false, totalCount: 1, ...meta },
        }),
      );

    it('거르지 않은 목록의 카드에는 거른 까닭을 붙이지 않는다', async () => {
      renderRoute('/?tab=project');

      const cards = await screen.findByRole('list', { name: '프로젝트' });
      expect(screen.queryByText('키워드로 찾은 결과')).not.toBeInTheDocument();
      expect(within(cards).queryByLabelText(/검색에 걸린/)).not.toBeInTheDocument();
    });

    it('왼쪽 주제로 거르면 목록 위에 주제를 강조하고 카드마다 반복하지 않는다', async () => {
      renderRoute('/?tab=project&topic=트랜잭션/데드락');

      expect(await screen.findByText('#트랜잭션/데드락', { selector: 'b' })).toBeInTheDocument();
      const cards = screen.getByRole('list', { name: '프로젝트' });
      expect(within(cards).queryByText('#트랜잭션/데드락')).not.toBeInTheDocument();
      // 그 주제를 고른 자료 탭으로 연다.
      const [first] = within(cards).getAllByRole('link');
      expect(first).toHaveAttribute(
        'href',
        expect.stringMatching(/^\/projects\/@[^/]+\/materials\?topic=/),
      );
    });

    it('검색어를 서버가 주제로 해석하면 검색어 아래에 그 주제를 알려 준다', async () => {
      server.use(
        projectList({ matchedTopic: { topicId: 'lock', name: '동시성', side: 'BACKEND' } }),
      );
      renderRoute('/?tab=project&q=락');

      await screen.findByRole('link', { name: /Dropit/ });
      expect(screen.getByText('키워드로 찾은 결과', { exact: false })).toHaveTextContent(
        '락 키워드로 찾은 결과',
      );
      expect(screen.getByText('#동시성')).toBeInTheDocument();
      expect(screen.getByText(/주제를 다룬 프로젝트를/)).toBeInTheDocument();
    });

    it('검색어로 찾으면 프로젝트가 걸린 근거 자료를 출처와 제목으로 보여 준다', async () => {
      server.use(
        projectList(
          {},
          {
            matchedResource: {
              resourceId: 7,
              source: 'github-pr',
              title: '레디스 분산 락으로 중복 드롭 막기',
            },
          },
        ),
      );
      renderRoute('/?tab=project&q=분산 락');

      const card = await screen.findByRole('link', { name: /Dropit/ });
      // 근거 자료로 찾은 카드는 주제를 모르니 지금처럼 경험 탭으로 연다.
      expect(card).toHaveAttribute('href', '/projects/@dropit');
      const evidence = within(card).getByLabelText(
        "'분산 락' 검색에 걸린 PR: 레디스 분산 락으로 중복 드롭 막기",
      );
      expect(within(evidence).getByText('PR')).toBeInTheDocument();
      expect(within(evidence).getByText('레디스 분산 락으로 중복 드롭 막기')).toBeInTheDocument();
    });
  });

  it('좋아요순으로 바꾸면 좋아요가 많은 프로젝트부터 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/?tab=project');

    const sort = await screen.findByRole('group', { name: '정렬' });
    await user.click(within(sort).getByRole('button', { name: '좋아요순' }));

    await waitFor(() =>
      expect(within(sort).getByRole('button', { name: '좋아요순' })).toHaveAttribute(
        'aria-pressed',
        'true',
      ),
    );
    const cards = await screen.findByRole('list', { name: '프로젝트' });
    await waitFor(() =>
      expect(within(cards).getAllByRole('link')[0]).toHaveAccessibleName(/Dropit/),
    );
    expect(within(cards).getAllByRole('link')[1]).toHaveAccessibleName(/여기담/);
  });

  it('모바일 바텀시트에서 직군을 거쳐 주제를 고른다', async () => {
    const user = userEvent.setup();
    renderRoute('/?tab=project');

    await user.click(await screen.findByRole('button', { name: '기술 주제: 전체 주제' }));
    const sheet = await screen.findByRole('dialog', { name: '기술 주제' });
    expect(within(sheet).getByRole('status')).toHaveTextContent('선택됨전체');

    // vaul 시트는 jsdom에서 포인터 캡처를 못 해 user.click 대신 click 이벤트만 보낸다.
    fireEvent.click(await within(sheet).findByRole('button', { name: /^백엔드/ }));
    const topics = await within(sheet).findByRole('list', { name: '주제' });
    fireEvent.click(within(topics).getByRole('button', { name: /트랜잭션\/데드락/ }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await screen.findByRole('heading', { name: '프로젝트 1' })).toBeVisible();

    await user.click(screen.getByRole('button', { name: '기술 주제 해제' }));
    expect(await screen.findByRole('heading', { name: '프로젝트 13' })).toBeVisible();
  });

  it('맞는 프로젝트가 없으면 안내를 보여준다', async () => {
    renderRoute('/?tab=project&q=없는프로젝트123');

    expect(
      await screen.findByText("'없는프로젝트123'에 맞는 프로젝트를 아직 못 찾았어요."),
    ).toBeVisible();
  });
});
