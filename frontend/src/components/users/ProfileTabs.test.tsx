import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ProfileTabs } from '@/components/users/ProfileTabs';

const renderTabs = (props?: Partial<Parameters<typeof ProfileTabs>[0]>) => {
  const onChange = jest.fn();
  render(
    <ProfileTabs value="projects" projectCount={2} feedCount={18} onChange={onChange} {...props} />,
  );

  return onChange;
};

describe('ProfileTabs', () => {
  it('탭 이름과 개수를 함께 보여준다', () => {
    renderTabs();

    expect(screen.getByRole('tab', { name: '프로젝트 (2)' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '피드 (18)' })).toBeInTheDocument();
  });

  it('고른 탭을 선택 상태로 보여준다', () => {
    renderTabs({ value: 'feeds' });

    expect(screen.getByRole('tab', { name: '피드 (18)' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: '프로젝트 (2)' })).toHaveAttribute(
      'aria-selected',
      'false',
    );
  });

  it('다른 탭을 누르면 그 탭을 알린다', async () => {
    const user = userEvent.setup();
    const onChange = renderTabs();

    await user.click(screen.getByRole('tab', { name: '피드 (18)' }));

    expect(onChange).toHaveBeenCalledWith('feeds');
  });
});
