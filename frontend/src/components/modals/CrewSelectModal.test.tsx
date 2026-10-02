/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { setupServer } from 'msw/node';

import { handlers } from '@/api/mock/handlers';
import { CrewSelectModal } from '@/components/modals/CrewSelectModal';
import { type CrewSearchItem } from '@/types/project';

const server = setupServer(...handlers);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const renderModal = (initial: CrewSearchItem[] = []) => {
  const onApply = jest.fn();
  const onClose = jest.fn();
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });

  render(
    <QueryClientProvider client={client}>
      <CrewSelectModal initial={initial} onApply={onApply} onClose={onClose} />
    </QueryClientProvider>,
  );

  return { onApply, onClose };
};

describe('CrewSelectModal', () => {
  it('검색 전에는 검색하라고 안내한다', () => {
    renderModal();

    expect(screen.getByText('이름이나 닉네임으로 크루를 검색해 주세요.')).toBeInTheDocument();
    expect(screen.queryByRole('list', { name: '크루 검색 결과' })).not.toBeInTheDocument();
  });

  it('검색한 크루를 결과로 보여준다', async () => {
    const user = userEvent.setup();
    renderModal();

    await user.type(screen.getByRole('searchbox', { name: '크루 검색' }), '재키');

    const results = await screen.findByRole('list', { name: '크루 검색 결과' });
    expect(within(results).getByRole('checkbox', { name: /재키/ })).toBeInTheDocument();
  });

  it('고른 크루를 칩으로 모아 보여준다', async () => {
    const user = userEvent.setup();
    renderModal();

    await user.type(screen.getByRole('searchbox', { name: '크루 검색' }), '재키');
    const results = await screen.findByRole('list', { name: '크루 검색 결과' });
    await user.click(within(results).getByRole('checkbox', { name: /재키/ }));

    const collector = await screen.findByRole('region', { name: '선택된 팀원' });
    expect(within(collector).getByRole('button', { name: '재키 선택 해제' })).toBeInTheDocument();
  });

  it('적용하면 고른 크루를 통째로 넘긴다', async () => {
    const user = userEvent.setup();
    const { onApply } = renderModal();

    await user.type(screen.getByRole('searchbox', { name: '크루 검색' }), '재키');
    const results = await screen.findByRole('list', { name: '크루 검색 결과' });
    await user.click(within(results).getByRole('checkbox', { name: /재키/ }));
    await user.click(screen.getByRole('button', { name: '1명 팀원 추가하기' }));

    expect(onApply).toHaveBeenCalledWith([expect.objectContaining({ handle: 'zzaekkii' })]);
  });

  it('검색어를 바꿔도 이미 고른 크루는 남는다', async () => {
    const user = userEvent.setup();
    renderModal();

    const search = screen.getByRole('searchbox', { name: '크루 검색' });
    await user.type(search, '재키');
    const results = await screen.findByRole('list', { name: '크루 검색 결과' });
    await user.click(within(results).getByRole('checkbox', { name: /재키/ }));

    await user.clear(search);
    await user.type(search, '두리');

    const collector = await screen.findByRole('region', { name: '선택된 팀원' });
    expect(within(collector).getByRole('button', { name: '재키 선택 해제' })).toBeInTheDocument();
  });
});
