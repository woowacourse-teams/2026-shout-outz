/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen } from '@testing-library/react';

import { magazineHandlers } from '@/mocks/magazines';
import { renderRoute, server } from '@/test/renderRoute';

beforeEach(() => server.use(...magazineHandlers));

it('매거진 본문을 마크다운으로 그린다', async () => {
  renderRoute('/magazines/1');

  expect(
    await screen.findByRole('heading', { level: 1, name: '우진의 레벨 3 회고' }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('heading', { level: 2, name: '자기소개 부탁드려요' }),
  ).toBeInTheDocument();
  expect(screen.getByText('우진', { selector: 'strong' })).toBeInTheDocument();
  expect(screen.getByRole('img', { name: '프로젝트 회고 사진' })).toBeInTheDocument();
  expect(screen.getAllByRole('listitem')).toHaveLength(5);
});

it('이미지 title은 캡션으로, 태그의 color는 정해둔 색으로 보여준다', async () => {
  renderRoute('/magazines/1');

  expect(await screen.findByText('팀원들과 레벨 3 회고를 마친 날')).toBeInTheDocument();
  expect(screen.getByText('실제 사용자')).toHaveClass('text-red-600!');
  expect(screen.getByText('꾸준히')).toHaveClass('text-primary-600!');
});

it('매거진 탭 목록에서 카드를 누르면 상세로 이동한다', async () => {
  renderRoute('/magazines');

  expect(await screen.findByRole('link', { name: '첫 배포를 마친 팀 이야기' })).toHaveAttribute(
    'href',
    '/magazines/3',
  );
});
