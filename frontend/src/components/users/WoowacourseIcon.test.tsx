import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { WoowacourseIcon } from '@/components/users/WoowacourseIcon';

it('마우스 호버와 키보드 포커스로 설명을 열고 Escape로 닫는다', async () => {
  const user = userEvent.setup();
  render(<WoowacourseIcon userType="WOOWACOURSE_CREW" />);
  const icon = screen.getByRole('button', { name: '우아한테크코스 소속' });
  await user.hover(icon);
  expect(screen.getByRole('tooltip')).toHaveTextContent('소속 인증을 완료한 사용자');
  await user.unhover(icon);
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  await user.tab();
  expect(icon).toHaveFocus();
  expect(screen.getByRole('tooltip')).toBeInTheDocument();
  await user.keyboard('{Escape}');
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
});

it('터치로 설명을 토글하고 부모 링크로 이동하지 않는다', async () => {
  const user = userEvent.setup();
  const navigate = jest.fn();
  render(
    <a href="/users/crew" onClick={navigate}>
      <WoowacourseIcon userType="WOOWACOURSE_CREW" />
    </a>,
  );
  const icon = screen.getByRole('button', { name: '우아한테크코스 소속' });
  await user.pointer([{ keys: '[TouchA>]', target: icon }, { keys: '[/TouchA]' }]);
  expect(screen.getByRole('tooltip')).toBeInTheDocument();
  expect(navigate).not.toHaveBeenCalled();
  await user.pointer([{ keys: '[TouchA>]', target: icon }, { keys: '[/TouchA]' }]);
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  await user.pointer([{ keys: '[TouchA>]', target: icon }, { keys: '[/TouchA]' }]);
  fireEvent.pointerDown(document.body);
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
});

it('모달 안에서는 설명을 같은 dialog 안에 렌더링하고 Escape가 모달에 전달되지 않는다', async () => {
  const user = userEvent.setup();
  const closeModal = jest.fn();
  render(
    <dialog open onKeyDown={closeModal}>
      <WoowacourseIcon userType="WOOWACOURSE_COACH" />
    </dialog>,
  );
  await user.hover(screen.getByRole('button', { name: '우아한테크코스 소속' }));
  expect(screen.getByRole('dialog')).toContainElement(screen.getByRole('tooltip'));
  await user.keyboard('{Escape}');
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  expect(closeModal).not.toHaveBeenCalled();
});

it('선택 버튼 안에서도 아이콘 탭은 선택이나 해제를 실행하지 않는다', async () => {
  const user = userEvent.setup();
  const select = jest.fn();
  render(
    <button onClick={select}>
      <WoowacourseIcon userType="WOOWACOURSE_CREW" />
    </button>,
  );
  const icon = screen.getByRole('button', { name: '우아한테크코스 소속' });
  await user.pointer([{ keys: '[TouchA>]', target: icon }, { keys: '[/TouchA]' }]);
  expect(screen.getByRole('tooltip')).toBeInTheDocument();
  expect(select).not.toHaveBeenCalled();
});
