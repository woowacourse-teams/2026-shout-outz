/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, within } from '@testing-library/react';

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
  expect(screen.getByText('실제 사용자')).toHaveStyle({ color: 'var(--adaptive-red-600)' });
  expect(screen.getByText('꾸준히')).toHaveStyle({ color: 'var(--adaptive-blue-600)' });
  expect(screen.getByText('Q. 레벨 3 프로젝트에서 가장 기억에 남는 순간은?')).toHaveStyle({
    color: 'var(--static-blue-500)',
  });
});

it('매거진 탭 목록에서 카드를 누르면 상세로 이동한다', async () => {
  renderRoute('/magazines');

  expect(await screen.findByRole('link', { name: '첫 배포를 마친 팀 이야기' })).toHaveAttribute(
    'href',
    '/magazines/3',
  );
});

it('상단에는 에디터를, 본문 앞에는 인터뷰이와 소개문을 보여준다', async () => {
  renderRoute('/magazines/1');

  expect(await screen.findByText('에디터')).toBeInTheDocument();
  expect(screen.getByText('재키')).toBeInTheDocument();

  const intro = screen.getByRole('region', { name: '인터뷰이 소개' });
  expect(within(intro).getByRole('link', { name: '우진 프로필 보기' })).toHaveAttribute(
    'href',
    '/users/crew1',
  );
  expect(within(intro).getByText('우아한테크코스 8기')).toBeInTheDocument();
});
