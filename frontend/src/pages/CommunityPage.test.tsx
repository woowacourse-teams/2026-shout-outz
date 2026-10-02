/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, within } from '@testing-library/react';

import { renderRoute } from '@/test/renderRoute';

test('커뮤니티 주소에 유형이 없으면 첫 번째 질문 탭을 연다', async () => {
  renderRoute('/community');

  const tabs = await screen.findByRole('tablist', { name: '커뮤니티 유형' });
  expect(within(tabs).getByRole('tab', { name: '질문' })).toHaveAttribute('aria-selected', 'true');
  expect(await screen.findByRole('button', { name: '질문하기' })).toBeInTheDocument();
  expect(
    await screen.findByRole('link', { name: /Server-Sent Events\(SSE\)/ }),
  ).toBeInTheDocument();
});

test('피드 유형을 지정한 주소는 피드 탭을 연다', async () => {
  renderRoute('/community?type=POST');

  const tabs = await screen.findByRole('tablist', { name: '커뮤니티 유형' });
  expect(within(tabs).getByRole('tab', { name: '피드' })).toHaveAttribute('aria-selected', 'true');
  expect(await screen.findByRole('button', { name: '글쓰기' })).toBeInTheDocument();
});
