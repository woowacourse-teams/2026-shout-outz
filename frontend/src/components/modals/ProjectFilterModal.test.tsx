/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderRoute, server } from '@/test/renderRoute';

const openFilterModal = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.click(await screen.findByRole('button', { name: /상세 필터/ }));
  return screen.findByRole('heading', { name: '프로젝트 상세 필터' });
};

describe('프로젝트 상세 필터', () => {
  it('기술 스택과 기수를 탭으로 나눠 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/projects');

    await openFilterModal(user);

    expect(await screen.findByRole('list', { name: '기술 스택 필터' })).toBeInTheDocument();

    await user.click(screen.getByRole('tab', { name: '기수' }));

    expect(await screen.findByRole('list', { name: '기수 필터' })).toBeInTheDocument();
  });

  it('선택지마다 적용했을 때 남는 프로젝트 수를 함께 보여준다', async () => {
    const user = userEvent.setup();
    renderRoute('/projects');

    await openFilterModal(user);
    const list = await screen.findByRole('list', { name: '기술 스택 필터' });

    expect(within(list).getByRole('checkbox', { name: /React/ })).toHaveTextContent('개');
  });

  it('고른 조건을 칩으로 모아 보여주고 지울 수 있다', async () => {
    const user = userEvent.setup();
    renderRoute('/projects');

    await openFilterModal(user);
    const list = await screen.findByRole('list', { name: '기술 스택 필터' });
    await user.click(within(list).getByRole('checkbox', { name: /React/ }));

    const collector = await screen.findByRole('region', { name: '선택된 필터' });
    expect(within(collector).getByText(/React/)).toBeInTheDocument();

    await user.click(within(collector).getByRole('button', { name: /선택 해제/ }));

    expect(screen.queryByRole('region', { name: '선택된 필터' })).not.toBeInTheDocument();
  });

  it('적용하면 고른 조건이 URL에 남는다', async () => {
    const user = userEvent.setup();
    const router = renderRoute('/projects');

    await openFilterModal(user);
    const list = await screen.findByRole('list', { name: '기술 스택 필터' });
    await user.click(within(list).getByRole('checkbox', { name: /React/ }));
    await user.click(screen.getByRole('button', { name: /프로젝트 보기/ }));

    await waitFor(() => expect(router.state.location.search).toHaveProperty('techTagIds', [1]));
  });

  it('그냥 닫으면 고르던 것이 반영되지 않는다', async () => {
    const user = userEvent.setup();
    const router = renderRoute('/projects');

    await openFilterModal(user);
    const list = await screen.findByRole('list', { name: '기술 스택 필터' });
    await user.click(within(list).getByRole('checkbox', { name: /React/ }));
    await user.click(screen.getByRole('button', { name: '닫기' }));

    await waitFor(() =>
      expect(screen.queryByRole('heading', { name: '프로젝트 상세 필터' })).not.toBeInTheDocument(),
    );
    expect(router.state.location.search).not.toHaveProperty('techTagIds');
  });
});

describe('프로젝트 검색', () => {
  it('검색어를 넣고 제출하면 URL에 남는다', async () => {
    const user = userEvent.setup();
    const router = renderRoute('/projects');

    await user.type(await screen.findByRole('searchbox', { name: '프로젝트 검색' }), '블록체인');
    await user.click(screen.getByRole('button', { name: '검색' }));

    await waitFor(() => expect(router.state.location.search).toHaveProperty('keyword', '블록체인'));
  });

  it('조건에 맞는 결과가 없으면 초기화할 길을 준다', async () => {
    server.use(
      http.get('/api/v1/projects', () =>
        HttpResponse.json({
          status: 'success',
          data: [],
          meta: { nextCursor: null, hasNext: false, totalCount: 0 },
        }),
      ),
    );
    const user = userEvent.setup();
    const router = renderRoute('/projects?keyword=블록체인');

    expect(await screen.findByText('검색 결과가 없습니다.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '검색 및 필터 초기화' }));

    await waitFor(() => expect(router.state.location.search).not.toHaveProperty('keyword'));
  });
});

describe('필터 선택지 다시 불러오기', () => {
  it('고르는 동안 목록이 사라지지 않고 수만 바뀐다', async () => {
    const user = userEvent.setup();
    renderRoute('/projects');

    await openFilterModal(user);
    const list = await screen.findByRole('list', { name: '기술 스택 필터' });
    expect(screen.getByRole('button', { name: '3개 프로젝트 보기' })).toBeInTheDocument();

    await user.click(within(list).getByRole('checkbox', { name: /React/ }));

    // 목록이 "불러오는 중"으로 갈리지 않고 자리를 지킨다.
    expect(screen.getByRole('list', { name: '기술 스택 필터' })).toBeInTheDocument();
    expect(screen.queryByText('필터를 불러오는 중…')).not.toBeInTheDocument();

    await waitFor(() =>
      expect(screen.getByRole('button', { name: '2개 프로젝트 보기' })).toBeInTheDocument(),
    );
  });
});
