import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { TechTagField } from '@/components/projects/TechTagField';

const REACT_TAG = { id: 1, displayName: 'React' };

describe('TechTagField', () => {
  it('고른 기술 스택을 칩으로 보여준다', () => {
    render(<TechTagField value={[REACT_TAG]} onChange={jest.fn()} />);

    expect(screen.getByText('React')).toBeInTheDocument();
  });

  it('칩을 지우면 그 기술 스택만 빠진다', async () => {
    const user = userEvent.setup();
    const onChange = jest.fn();
    render(
      <TechTagField
        value={[REACT_TAG, { id: 2, displayName: 'TypeScript' }]}
        onChange={onChange}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'React 삭제' }));

    expect(onChange).toHaveBeenCalledWith([{ id: 2, displayName: 'TypeScript' }]);
  });

  it('오류 문구를 받으면 보여준다', () => {
    render(
      <TechTagField value={[]} onChange={jest.fn()} error="기술 스택을 1개 이상 선택해 주세요." />,
    );

    expect(screen.getByText('기술 스택을 1개 이상 선택해 주세요.')).toBeInTheDocument();
  });
});
