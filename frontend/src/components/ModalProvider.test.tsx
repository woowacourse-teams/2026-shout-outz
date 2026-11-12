import { fireEvent, render, screen } from '@testing-library/react';
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

  /**
   * 브라우저는 문서에 붙어 있는 dialog를 닫을 때만, 열기 직전에 포커스가 있던 요소로 포커스를 돌려준다.
   * jsdom은 이 복원을 구현하지 않으므로, 복원의 전제인 "DOM에서 빼기 전에 닫는다"는 순서만 확인한다.
   * 실제 포커스 복원은 브라우저에서 확인했다.
   */
  describe('닫는 순서', () => {
    let closedWhileConnected: boolean[];

    beforeEach(() => {
      closedWhileConnected = [];
      const close = HTMLDialogElement.prototype.close;
      jest.spyOn(HTMLDialogElement.prototype, 'close').mockImplementation(function (
        this: HTMLDialogElement,
        returnValue?: string,
      ) {
        if (this.open) closedWhileConnected.push(this.isConnected);
        close.call(this, returnValue);
      });
    });

    afterEach(() => jest.restoreAllMocks());

    it('모달 안의 버튼으로 닫으면 DOM에서 빼기 전에 dialog를 닫는다', async () => {
      const user = userEvent.setup();
      renderHost();

      await user.click(screen.getByRole('button', { name: '열기' }));
      await user.click(screen.getByRole('button', { name: '사과 고르기' }));

      expect(closedWhileConnected).toEqual([true]);
    });

    it('Escape로 닫아도 DOM에서 빼기 전에 dialog를 닫는다', async () => {
      const user = userEvent.setup();
      renderHost();

      await user.click(screen.getByRole('button', { name: '열기' }));
      // jsdom은 Escape로 cancel 이벤트를 만들지 않아, 브라우저가 보내는 cancel 이벤트를 직접 보낸다.
      fireEvent(screen.getByRole('dialog'), new Event('cancel'));

      expect(screen.queryByRole('button', { name: '사과 고르기' })).not.toBeInTheDocument();
      expect(closedWhileConnected).toEqual([true]);
    });
  });
});
