/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { act, screen, within } from '@testing-library/react';
import { renderRoute } from '@/test/renderRoute';

// precedence를 준 스타일시트는 로드될 때까지 화면을 숨긴 채 보류한다.
// jsdom은 CSS를 받지 않으므로 링크가 생기면 load 이벤트로 로드를 끝낸다.
async function loadAboutStylesheets() {
  const selector = 'link[href^="/about-assets/"]';
  for (let i = 0; i < 50 && !document.head.querySelector(selector); i += 1) {
    await act(() => new Promise((resolve) => setTimeout(resolve, 100)));
  }
  await act(async () => {
    document.head
      .querySelectorAll(selector)
      .forEach((link) => link.dispatchEvent(new Event('load')));
  });
}

test('랜딩의 프로젝트는 목록 API에서 받아 상세 페이지로 연결한다', async () => {
  renderRoute('/about');
  await loadAboutStylesheets();
  const projects = await screen.findByRole('list', { name: '최신 프로젝트' });
  const cards = within(projects).getAllByRole('article');
  expect(cards.length).toBeGreaterThan(0);
  expect(cards.length).toBeLessThanOrEqual(3);
  expect(within(projects).getByRole('heading', { name: 'Dropit' })).toBeInTheDocument();
  for (const link of within(projects).getAllByRole('link')) {
    expect(link.getAttribute('href')).toMatch(/^\/projects\/.+/);
  }
  expect(screen.getByRole('link', { name: /프로젝트 전체 보기/ })).toHaveAttribute(
    'href',
    '/projects',
  );
});
