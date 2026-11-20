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

test('/about 본문의 예시 링크는 프로토타입으로 연결된다', async () => {
  renderRoute('/about');
  await loadAboutStylesheets();
  const prototypeUrl = 'https://project-after-psi.vercel.app/#/projects';
  const projects = await screen.findByRole('list', { name: '추천 프로젝트' });
  for (const link of within(projects).getAllByRole('link'))
    expect(link).toHaveAttribute('href', prototypeUrl);
  for (const link of within(screen.getByRole('main')).getAllByRole('link'))
    expect(link.getAttribute('href')).not.toMatch(/^\/(projects|community|magazines|news)(\/|$)/);
});

test('/about에서도 서비스 GNB와 푸터로 다른 화면에 갈 수 있다', async () => {
  renderRoute('/about');
  await loadAboutStylesheets();
  const banner = await screen.findByRole('banner');

  expect(within(banner).getByRole('link', { name: 'shout-outz' })).toHaveAttribute('href', '/');
  const nav = within(banner).getByRole('navigation', { name: '서비스 메뉴' });
  expect(
    within(nav)
      .getAllByRole('link')
      .map((link) => [link.textContent, link.getAttribute('href')]),
  ).toEqual([
    ['탐색', '/'],
    ['커뮤니티', '/community'],
    ['매거진', '/magazines'],
    ['소식', '/news'],
  ]);
  expect(banner.querySelector('[inert]')).toBeNull();

  const footer = screen.getByRole('navigation', { name: '서비스 바로가기' });
  expect(within(footer).getByRole('link', { name: '커뮤니티' })).toHaveAttribute(
    'href',
    '/community',
  );
});
