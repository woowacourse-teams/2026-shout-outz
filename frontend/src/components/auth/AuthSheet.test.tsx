import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { AuthSheet } from '@/components/auth/AuthSheet';

describe('AuthSheet', () => {
  it('환영 문구 대신 로그인 창이라는 목적을 모달 이름으로 알린다', () => {
    render(<AuthSheet onClose={jest.fn()} />);

    expect(screen.getByRole('dialog', { name: '로그인' })).toHaveAccessibleDescription(
      '궁금한 건 편하게 묻고, 서로의 이야기에 응원을 보내 보세요.',
    );
  });

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
