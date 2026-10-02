import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Modal } from '@/components/Modal';
import { ModalProvider } from '@/components/ModalProvider';
import { useModal } from '@/hooks/useModal';

/** 모달을 열고 결과를 화면에 적는 최소 화면. Promise로 값이 돌아오는지를 본다. */
function Host() {
  const { open } = useModal();

  return (
    <>
      <button
        type="button"
        onClick={async () => {
          const picked = await open<string>((close) => (
            <Modal onClose={() => close('취소')}>
              <button type="button" onClick={() => close('사과')}>
                사과 고르기
              </button>
              <button type="button" onClick={() => close('배')}>
                배 고르기
              </button>
            </Modal>
          ));
          document.title = picked ?? '없음';
        }}
      >
        열기
      </button>
      <button
        type="button"
        onClick={() => void open<void>((close) => <Modal onClose={() => close()}>둘째</Modal>)}
      >
        둘째 열기
      </button>
    </>
  );
}

const renderHost = () =>
  render(
    <ModalProvider>
      <Host />
    </ModalProvider>,
  );

describe('ModalProvider', () => {
  it('열기 전에는 아무것도 그리지 않는다', () => {
    renderHost();

    expect(screen.queryByRole('button', { name: '사과 고르기' })).not.toBeInTheDocument();
  });

  it('고른 값을 연 쪽으로 돌려준다', async () => {
    const user = userEvent.setup();
    renderHost();

    await user.click(screen.getByRole('button', { name: '열기' }));
    await user.click(screen.getByRole('button', { name: '배 고르기' }));

    expect(document.title).toBe('배');
  });

  it('닫은 모달은 화면에서 사라진다', async () => {
    const user = userEvent.setup();
    renderHost();

    await user.click(screen.getByRole('button', { name: '열기' }));
    await user.click(screen.getByRole('button', { name: '사과 고르기' }));

    expect(screen.queryByRole('button', { name: '사과 고르기' })).not.toBeInTheDocument();
  });

  it('모달 위에 모달을 겹쳐 열 수 있다', async () => {
    const user = userEvent.setup();
    renderHost();

    await user.click(screen.getByRole('button', { name: '열기' }));
    await user.click(screen.getByRole('button', { name: '둘째 열기' }));

    expect(screen.getByRole('button', { name: '사과 고르기' })).toBeInTheDocument();
    expect(screen.getByText('둘째')).toBeInTheDocument();
  });
});
