import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Dropdown } from '@/components/Dropdown';

function show(onSelect = jest.fn()) {
  render(
    <>
      <Dropdown trigger="···" aria-label="피드 메뉴">
        <Dropdown.Item onSelect={onSelect}>수정</Dropdown.Item>
        <Dropdown.Item onSelect={onSelect} disabled>
          비활성 항목
        </Dropdown.Item>
        <Dropdown.Item onSelect={onSelect}>삭제</Dropdown.Item>
      </Dropdown>
      <button>바깥 버튼</button>
    </>,
  );
  return screen.getByRole('button', { name: '피드 메뉴' });
}

test('선택하면 실행하고 메뉴를 닫는다', async () => {
  const user = userEvent.setup();
  const onSelect = jest.fn();
  const trigger = show(onSelect);
  await user.click(trigger);
  await user.click(screen.getByRole('menuitem', { name: '수정' }));
  expect(onSelect).toHaveBeenCalledTimes(1);
  expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  expect(trigger).toHaveAttribute('aria-expanded', 'false');
});

test('열면 항목 대신 메뉴에 포커스를 준다', async () => {
  const user = userEvent.setup();
  await user.click(show());
  expect(screen.getByRole('menu')).toHaveFocus();
  expect(screen.getByRole('menuitem', { name: '수정' })).not.toHaveFocus();
});

test('비활성 항목은 실행하지 않고 바깥 클릭은 메뉴를 닫는다', async () => {
  const user = userEvent.setup();
  const onSelect = jest.fn();
  await user.click(show(onSelect));
  await user.click(screen.getByRole('menuitem', { name: '비활성 항목' }));
  expect(onSelect).not.toHaveBeenCalled();
  expect(screen.getByRole('menu')).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: '바깥 버튼' }));
  expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: '바깥 버튼' })).toHaveFocus();
});

test('Tab은 다음 요소로 이동하면서 메뉴를 닫는다', async () => {
  const user = userEvent.setup();
  await user.click(show());
  await user.tab();
  expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: '바깥 버튼' })).toHaveFocus();
});
