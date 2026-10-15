import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { AuthSheet } from '@/components/auth/AuthSheet';

describe('AuthSheet', () => {
  it('GitHub 로그인을 시작하는 버튼을 보여준다', () => {
    render(<AuthSheet onClose={jest.fn()} />);

    expect(screen.getByRole('button', { name: 'GitHub 계정으로 시작하기' })).toBeEnabled();
  });

  it('둘러보기를 고르면 그냥 닫는다', async () => {
    const user = userEvent.setup();
    const onClose = jest.fn();
    render(<AuthSheet onClose={onClose} />);

    await user.click(screen.getByRole('button', { name: '로그인 없이 둘러보기' }));

    expect(onClose).toHaveBeenCalled();
  });

  it('우아한테크코스 소속 인증 문구 없이 환영 제목을 보여준다', () => {
    render(<AuthSheet onClose={jest.fn()} />);

    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent(
      '샤라웃에 오신 것을 환영해요',
    );
    expect(screen.getByText('샤라웃')).toHaveClass('text-primary-600');
    expect(screen.queryByText(/우아한테크코스 소속 인증/)).not.toBeInTheDocument();
  });
});
