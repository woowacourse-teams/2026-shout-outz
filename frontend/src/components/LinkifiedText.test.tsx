import { render, screen } from '@testing-library/react';

import { LinkifiedText } from '@/components/LinkifiedText';

describe('LinkifiedText', () => {
  it('주소를 새 탭으로 열리는 링크로 바꾼다', () => {
    render(<LinkifiedText text="참고: https://blog.test/redis?page=1 확인해 보세요" />);

    const link = screen.getByRole('link', { name: 'https://blog.test/redis?page=1' });
    expect(link).toHaveAttribute('href', 'https://blog.test/redis?page=1');
    expect(link).toHaveAttribute('target', '_blank');
  });

  it('주소 끝에 붙은 문장부호는 링크에서 뺀다', () => {
    render(<LinkifiedText text="(https://a.test/docs)." />);

    expect(screen.getByRole('link')).toHaveAttribute('href', 'https://a.test/docs');
  });

  it('주소가 없으면 글자만 보여준다', () => {
    render(<LinkifiedText text="**굵게** 안 바뀌어요" />);

    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    expect(screen.getByText('**굵게** 안 바뀌어요')).toBeInTheDocument();
  });
});
