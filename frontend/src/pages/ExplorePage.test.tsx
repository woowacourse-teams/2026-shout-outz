/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
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

  it('처음에는 키워드 탭을 고르고, 모든 탭을 누를 수 있다', async () => {
    renderRoute('/explore');

    const tabs = await screen.findByRole('tablist', { name: '찾는 방법' });
    expect(within(tabs).getByRole('tab', { name: '키워드' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    expect(within(tabs).getByRole('tab', { name: '채용공고' })).toBeEnabled();
    expect(within(tabs).getByRole('tab', { name: '프로젝트' })).toBeEnabled();
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

describe('ExplorePage 채용공고 탭', () => {
  it('채용공고 탭을 누르면 공고 카드 목록을 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/explore');

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
    expect(within(cards).getByRole('img', { name: '토스뱅크 로고' })).toHaveAttribute(
      'src',
      expect.stringContaining('data:image/svg+xml'),
    );
    expect(within(cards).getByText('#Product')).toBeVisible();
    expect(screen.getByRole('searchbox', { name: '채용공고 검색' })).toBeVisible();
  });

  it('추천 검색어로 공고를 찾으면 맞는 공고만 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/explore?tab=job');

    const suggestions = await screen.findByRole('list', { name: '추천 검색어' });
    await user.click(within(suggestions).getByRole('button', { name: '토스증권' }));

    const cards = await screen.findByRole('list', { name: '공고' });
    expect(await within(cards).findAllByRole('link')).toHaveLength(1);
    expect(within(cards).getByRole('link', { name: /Frontend Developer/ })).toBeVisible();
    expect(within(cards).queryByText('곧 추가될 공고예요')).not.toBeInTheDocument();
  });

  it('맞는 공고가 없으면 안내를 보여준다', async () => {
    renderRoute('/explore?tab=job&q=없는회사123');

    expect(await screen.findByText("'없는회사123'에 맞는 공고를 아직 못 찾았어요.")).toBeVisible();
  });
});

describe('ExplorePage 프로젝트 탭', () => {
  it('프로젝트 탭을 누르면 프로젝트 카드 목록과 기술 주제를 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/explore');

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

  it('기술 주제를 고르면 그 주제를 다룬 프로젝트만 보여주고, 다시 누르면 푼다', async () => {
    const user = userEvent.setup();
    renderRoute('/explore?tab=project');

    const nav = await screen.findByRole('navigation', { name: '기술 주제' });
    await user.click(within(nav).getByRole('button', { name: 'BE' }));
    const topic = within(nav).getByRole('button', { name: /트랜잭션\/데드락/ });
    await user.click(topic);

    expect(await screen.findByRole('heading', { name: '프로젝트 1' })).toBeVisible();
    const cards = screen.getByRole('list', { name: '프로젝트' });
    expect(within(cards).getByRole('link', { name: /Shout-outz/ })).toBeVisible();
    expect(within(cards).queryByRole('link', { name: /Dropit/ })).not.toBeInTheDocument();
    expect(topic).toHaveAttribute('aria-pressed', 'true');

    await user.click(topic);
    expect(await screen.findByRole('heading', { name: '프로젝트 13' })).toBeVisible();
  });

  it('추천 주제를 누르면 기술 주제로 거른다', async () => {
    const user = userEvent.setup();
    renderRoute('/explore?tab=project');

    const suggestions = await screen.findByRole('list', { name: '추천 검색어' });
    await user.click(within(suggestions).getByRole('button', { name: '트랜잭션/데드락' }));

    expect(await screen.findByRole('heading', { name: '프로젝트 1' })).toBeVisible();
    expect(within(suggestions).getByRole('button', { name: '트랜잭션/데드락' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });

  it('좋아요순으로 바꾸면 좋아요가 많은 프로젝트부터 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/explore?tab=project');

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
    renderRoute('/explore?tab=project');

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
    renderRoute('/explore?tab=project&q=없는프로젝트123');

    expect(
      await screen.findByText("'없는프로젝트123'에 맞는 프로젝트를 아직 못 찾았어요."),
    ).toBeVisible();
  });
});
