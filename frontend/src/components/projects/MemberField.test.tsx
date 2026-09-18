import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { MemberField } from '@/components/projects/MemberField';

describe('MemberField', () => {
  it('고른 팀원을 칩으로 보여준다', () => {
    render(<MemberField value={['zzaekkii']} onChange={jest.fn()} />);

    expect(screen.getByText('zzaekkii')).toBeInTheDocument();
  });

  it('칩을 지우면 그 handle만 빠진다', async () => {
    const user = userEvent.setup();
    const onChange = jest.fn();
    render(<MemberField value={['zzaekkii', 'dhyepark']} onChange={onChange} />);

    await user.click(screen.getByRole('button', { name: 'zzaekkii 삭제' }));

    expect(onChange).toHaveBeenCalledWith(['dhyepark']);
  });

  it('오류 문구를 받으면 보여준다', () => {
    render(
      <MemberField value={[]} onChange={jest.fn()} error="참여 팀원을 1명 이상 선택해 주세요." />,
    );

    expect(screen.getByText('참여 팀원을 1명 이상 선택해 주세요.')).toBeInTheDocument();
  });
});
