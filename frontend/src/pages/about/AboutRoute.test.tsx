/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { act, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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

test('/about에서 추천 프로젝트를 누르면 실제 프로젝트 상세로 이동한다', async () => {
  const router = renderRoute('/about');
  await loadAboutStylesheets();
  const projects = await screen.findByRole('list', { name: '추천 프로젝트' });
  expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument();

  await userEvent.click(within(projects).getAllByRole('link')[0]!);
  expect(router.state.location.pathname).toBe('/projects/@rilog');
});
