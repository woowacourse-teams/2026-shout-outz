/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { setupServer } from 'msw/node';

import { handlers } from '@/api/mock/handlers';
import { TechTagSelectModal } from '@/components/modals/TechTagSelectModal';
import { type TechTag } from '@/types/project';

const server = setupServer(...handlers);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const REACT_TAG: TechTag = { id: 1, displayName: 'React' };

const renderModal = (initial: TechTag[] = []) => {
  const onApply = jest.fn();
  const onClose = jest.fn();
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });

  render(
    <QueryClientProvider client={client}>
      <TechTagSelectModal initial={initial} onApply={onApply} onClose={onClose} />
    </QueryClientProvider>,
  );

  return { onApply, onClose };
};

describe('TechTagSelectModal', () => {
  it('고를 수 있는 기술 스택을 보여준다', async () => {
    renderModal();

    const list = await screen.findByRole('list', { name: '기술 스택 목록' });

    expect(within(list).getByRole('checkbox', { name: 'React' })).toBeInTheDocument();
  });

  it('열릴 때 이미 고른 것은 선택된 채로 보여준다', async () => {
    renderModal([REACT_TAG]);

    const list = await screen.findByRole('list', { name: '기술 스택 목록' });

    expect(within(list).getByRole('checkbox', { name: 'React' })).toBeChecked();
  });

  it('검색하면 이름이 맞는 것만 남는다', async () => {
    const user = userEvent.setup();
    renderModal();
    await screen.findByRole('list', { name: '기술 스택 목록' });

    await user.type(screen.getByRole('searchbox', { name: '기술 스택 검색' }), 'Redis');

    const list = await screen.findByRole('list', { name: '기술 스택 목록' });
    await within(list).findByRole('checkbox', { name: 'Redis' });
    expect(within(list).queryByRole('checkbox', { name: 'React' })).not.toBeInTheDocument();
  });

  it('적용을 눌러야 고른 것이 밖으로 나간다', async () => {
    const user = userEvent.setup();
    const { onApply } = renderModal();

    const list = await screen.findByRole('list', { name: '기술 스택 목록' });
    await user.click(within(list).getByRole('checkbox', { name: 'React' }));
    await user.click(screen.getByRole('button', { name: '1개 스택 선택 완료' }));

    expect(onApply).toHaveBeenCalledWith([REACT_TAG]);
  });

  it('닫기는 고르던 것을 넘기지 않는다', async () => {
    const user = userEvent.setup();
    const { onApply, onClose } = renderModal();

    const list = await screen.findByRole('list', { name: '기술 스택 목록' });
    await user.click(within(list).getByRole('checkbox', { name: 'React' }));
    await user.click(screen.getByRole('button', { name: '닫기' }));

    expect(onClose).toHaveBeenCalled();
    expect(onApply).not.toHaveBeenCalled();
  });
});

describe('TechTagSelectModal 포커스', () => {
  it('열면 검색 입력에 바로 칠 수 있다', async () => {
    renderModal();

    await screen.findByRole('list', { name: '기술 스택 목록' });

    expect(screen.getByRole('searchbox', { name: '기술 스택 검색' })).toHaveFocus();
  });
});
