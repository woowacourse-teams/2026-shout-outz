/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderRoute } from '@/test/renderRoute';

test('/about에서 추천 프로젝트를 누르면 실제 프로젝트 상세로 이동한다', async () => {
  const router = renderRoute('/about');
  const projects = await screen.findByRole('list', { name: '추천 프로젝트' });
  expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument();

  await userEvent.click(within(projects).getAllByRole('link')[0]!);
  expect(router.state.location.pathname).toBe('/projects/@rilog');
});
