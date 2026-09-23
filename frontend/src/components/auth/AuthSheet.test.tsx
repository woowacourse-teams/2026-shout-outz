import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { AuthSheet } from '@/components/auth/AuthSheet';

describe('AuthSheet', () => {
  it('GitHub 로그인은 서버 OAuth 주소로 나가는 링크다', () => {
    render(<AuthSheet onClose={jest.fn()} />);

    expect(screen.getByRole('link', { name: 'GitHub 계정으로 시작하기' })).toHaveAttribute(
      'href',
      'http://localhost/oauth2/authorization/github',
    );
  });

  it('둘러보기를 고르면 그냥 닫는다', async () => {
    const user = userEvent.setup();
    const onClose = jest.fn();
    render(<AuthSheet onClose={onClose} />);

    await user.click(screen.getByRole('button', { name: '로그인 없이 둘러보기' }));

    expect(onClose).toHaveBeenCalled();
  });
});
