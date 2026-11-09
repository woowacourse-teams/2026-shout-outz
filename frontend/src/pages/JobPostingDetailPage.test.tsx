/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { renderRoute } from '@/test/renderRoute';

const requirementGroup = () => screen.findByRole('group', { name: '이 공고가 찾는 역량' });

describe('JobPostingDetailPage', () => {
  it('공고 제목, 회사, 원문 링크를 보여준다', async () => {
    renderRoute('/job-postings/2');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Frontend Developer' }),
    ).toBeVisible();
    expect(screen.getByText('토스증권 · 상시 채용')).toBeVisible();
    expect(screen.getByRole('link', { name: '공고 원문 보기 ↗' })).toHaveAttribute(
      'target',
      '_blank',
    );
  });

  it('처음에는 첫 번째 역량을 골라 그 역량의 자료를 보여준다', async () => {
    renderRoute('/job-postings/2');

    const group = await requirementGroup();
    expect(
      within(group).getByRole('button', { name: 'React, Next.js, TypeScript' }),
    ).toHaveAttribute('aria-pressed', 'true');
    expect(within(group).getByRole('button', { name: '상태관리' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );

    const documents = await screen.findByRole('region', { name: /^PR과 팀 문서/ });
    expect(
      within(documents).getAllByText('관련 역량 · React, Next.js, TypeScript').length,
    ).toBeGreaterThan(0);
  });

  it('역량을 여러 개 고르면 자료를 모으고, 모두 해제하면 안내를 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/job-postings/1');

    const group = await requirementGroup();
    await screen.findByRole('region', { name: /^블로그/ });
    await user.click(within(group).getByRole('button', { name: '동시성 제어' }));

    expect((await screen.findAllByText(/관련 역량 · .*동시성 제어/)).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/관련 역량 · Kotlin, Spring/).length).toBeGreaterThan(0);

    await user.click(within(group).getByRole('button', { name: 'Kotlin, Spring' }));
    await user.click(within(group).getByRole('button', { name: '동시성 제어' }));

    expect(
      await screen.findByText('역량을 하나 이상 고르면 관련 자료를 보여 드려요.'),
    ).toBeVisible();
  });

  it('추천 자료를 누르면 다른 화면처럼 자료 요약 모달을 연다', async () => {
    const user = userEvent.setup();
    renderRoute('/job-postings/2');

    const documents = await screen.findByRole('region', { name: /^PR과 팀 문서/ });
    await user.click(within(documents).getAllByRole('button', { name: /^PR/ })[0]!);

    const summary = await screen.findByRole('dialog');
    expect(within(summary).getByRole('link', { name: /^원문 보기:/ })).toHaveAttribute(
      'target',
      '_blank',
    );
    expect(await within(summary).findByRole('heading', { name: '기술 태그' })).toBeVisible();
  });

  it('자료가 없는 역량만 고르면 안내를 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/job-postings/2');

    const group = await requirementGroup();
    await screen.findByRole('region', { name: /^PR과 팀 문서/ });
    await user.click(within(group).getByRole('button', { name: '상태관리' }));
    await user.click(within(group).getByRole('button', { name: 'React, Next.js, TypeScript' }));

    expect(await screen.findByText('고른 역량에 맞는 자료가 아직 없어요.')).toBeVisible();
  });

  it('자료 종류 탭을 고르면 그 종류만 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/job-postings/2');

    const kinds = await screen.findByRole('tablist', { name: '자료 종류' });
    await user.click(within(kinds).getByRole('tab', { name: /테코톡/ }));

    expect(await screen.findByRole('region', { name: /^테코톡/ })).toBeVisible();
    expect(screen.queryByRole('region', { name: /^PR과 팀 문서/ })).not.toBeInTheDocument();
  });

  it('커뮤니티 고민을 보여주고 접을 수 있다', async () => {
    const user = userEvent.setup();
    renderRoute('/job-postings/2');

    const panel = await screen.findByRole('complementary', { name: /커뮤니티에서 나온 고민/ });
    expect(within(panel).getByRole('link', { name: /금융권 취업 방향성/ })).toHaveAttribute(
      'href',
      '/community/1',
    );

    await user.click(within(panel).getByRole('button', { name: '커뮤니티 패널 접기' }));
    expect(within(panel).queryByRole('link')).not.toBeInTheDocument();
    expect(within(panel).getByRole('button', { name: '커뮤니티 패널 펼치기' })).toBeVisible();
  });

  it('없는 공고면 오류를 보여준다', async () => {
    renderRoute('/job-postings/999');

    expect(await screen.findByRole('alert')).toBeVisible();
  });
});
