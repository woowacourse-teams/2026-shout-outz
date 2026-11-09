/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { renderRoute } from '@/test/renderRoute';

const resultSection = async (name: RegExp) => screen.findByRole('region', { name });

describe('ExplorePage', () => {
  it('헤더에서 탐색을 현재 페이지로 표시한다', async () => {
    renderRoute('/explore');

    const header = await screen.findByRole('banner');
    expect(within(header).getByRole('link', { name: '탐색' })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('검색 전에는 추천 콘텐츠와 키워드 목록을 보여준다', async () => {
    renderRoute('/explore');

    expect(await screen.findByRole('heading', { name: '이런 컨텐츠는 어때요?' })).toBeVisible();
    expect(screen.getByRole('heading', { name: '많이 다룬 키워드' })).toBeVisible();
    expect(screen.getByRole('heading', { name: '분야별 키워드' })).toBeVisible();
    expect(screen.getByText('전체 77개 주제를 분야별로 모았어요.')).toBeVisible();
  });

  it('채용공고와 프로젝트 탭은 보이지만 누를 수 없다', async () => {
    renderRoute('/explore');

    const tabs = await screen.findByRole('tablist', { name: '찾는 방법' });
    expect(within(tabs).getByRole('tab', { name: '키워드' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    expect(within(tabs).getByRole('tab', { name: '채용공고' })).toBeDisabled();
    expect(within(tabs).getByRole('tab', { name: '프로젝트' })).toBeDisabled();
  });

  it('검색어를 입력하면 맞는 주제의 자료를 종류별로 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/explore');

    await user.type(await screen.findByRole('searchbox', { name: '기술 이야기 검색' }), '동시성');
    await user.click(screen.getByRole('button', { name: '검색' }));

    expect(await screen.findByText('동시성 제어/락')).toBeVisible();
    expect(await resultSection(/^블로그/)).toBeVisible();
    expect(await resultSection(/^테코톡/)).toBeVisible();
    expect(await resultSection(/^PR과 팀 문서/)).toBeVisible();
  });

  it('추천 검색어를 누르면 그 검색어로 검색한다', async () => {
    const user = userEvent.setup();
    renderRoute('/explore');

    const suggestions = await screen.findByRole('list', { name: '추천 검색어' });
    await user.click(within(suggestions).getByRole('button', { name: '무중단 배포' }));

    expect(await screen.findByRole('searchbox', { name: '기술 이야기 검색' })).toHaveValue(
      '무중단 배포',
    );
    expect(await resultSection(/^블로그/)).toBeVisible();
  });

  it('자료 종류 탭을 고르면 그 종류만 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/explore?q=동시성');

    const kinds = await screen.findByRole('tablist', { name: '자료 종류' });
    await user.click(within(kinds).getByRole('tab', { name: /테코톡/ }));

    expect(await resultSection(/^테코톡/)).toBeVisible();
    expect(screen.queryByRole('region', { name: /^블로그/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('region', { name: /^PR과 팀 문서/ })).not.toBeInTheDocument();
  });

  it('맞는 주제가 없으면 안내를 보여준다', async () => {
    renderRoute('/explore?q=없는검색어123');

    expect(
      await screen.findByText("'없는검색어123'에 맞는 주제를 아직 못 찾았어요."),
    ).toBeVisible();
  });

  it('테코톡은 영상 링크를 새 탭으로 연다', async () => {
    renderRoute('/explore?q=동시성');

    const talks = await resultSection(/^테코톡/);
    const [firstTalk] = within(talks).getAllByRole('link');
    expect(firstTalk).toHaveAttribute('href', expect.stringContaining('youtu'));
    expect(firstTalk).toHaveAttribute('target', '_blank');
  });

  it('PR을 누르면 기술 태그와 문제 해결 경험이 있는 요약을 연다', async () => {
    const user = userEvent.setup();
    renderRoute('/explore?q=동시성');

    const documents = await resultSection(/^PR과 팀 문서/);
    await user.click(within(documents).getAllByRole('button', { name: /^PR/ })[0]!);

    const summary = await screen.findByRole('dialog');
    expect(within(summary).getByRole('link', { name: /^원문 보기:/ })).toHaveAttribute(
      'target',
      '_blank',
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

  it('블로그 요약에는 기술 태그와 문제 해결 경험이 없다', async () => {
    const user = userEvent.setup();
    renderRoute('/explore?q=동시성');

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
