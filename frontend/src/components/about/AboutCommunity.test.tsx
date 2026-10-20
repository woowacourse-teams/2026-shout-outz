import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AboutCommunity } from '@/components/about/AboutCommunity';
import type { AboutLinkProps } from '@/components/about/shared';

const Link = (props: AboutLinkProps) => <a {...props} />;

it('공통 Tab으로 미리보기를 바꾸고 방향키와 Home/End로 포커스를 이동한다', async () => {
  const user = userEvent.setup();
  render(<AboutCommunity LinkComponent={Link} />);
  const question = screen.getByRole('tab', { name: '질문', selected: true });
  expect(screen.getByRole('tabpanel', { name: '질문' })).toHaveTextContent(
    '프로젝트 경험, 면접에서 어떻게 이야기하나요?',
  );
  question.focus();
  await user.keyboard('{ArrowRight}');
  expect(screen.getByRole('tab', { name: '피드', selected: true })).toHaveFocus();
  expect(screen.getByRole('tabpanel', { name: '피드' })).toHaveTextContent(
    '첫 팀 프로젝트를 마치고',
  );
  await user.keyboard('{Home}');
  expect(screen.getByRole('tab', { name: '질문', selected: true })).toHaveFocus();
  await user.keyboard('{End}');
  expect(screen.getByRole('tab', { name: '피드', selected: true })).toHaveFocus();
});
