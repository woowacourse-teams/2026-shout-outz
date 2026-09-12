import { useRef, useState } from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Select } from '@/components/Select';

describe('Select', () => {
  describe('선택값 표시', () => {
    it('옵션을 선택하면 선택한 콘텐츠를 표시한다', async () => {
      const user = userEvent.setup();

      render(
        <Select aria-label="과일" placeholder="과일을 선택하세요">
          <Select.Item value="apple">사과</Select.Item>
          <Select.Item value="banana">바나나</Select.Item>
        </Select>,
      );

      await user.click(screen.getByRole('combobox', { name: '과일' }));
      await user.click(screen.getByRole('option', { name: '바나나' }));

      expect(screen.getByRole('combobox', { name: '과일' })).toHaveTextContent('바나나');
    });

    it('defaultValue에 해당하는 콘텐츠를 초깃값으로 표시한다', () => {
      render(
        <Select aria-label="과일" defaultValue="apple">
          <Select.Item value="apple">사과</Select.Item>
        </Select>,
      );

      expect(screen.getByRole('combobox', { name: '과일' })).toHaveTextContent('사과');
    });
  });

  describe('제어 상태', () => {
    it('value가 제공되면 사용자 선택으로 표시값을 직접 변경하지 않는다', async () => {
      const user = userEvent.setup();

      render(
        <Select aria-label="과일" value="apple">
          <Select.Item value="apple">사과</Select.Item>
          <Select.Item value="banana">바나나</Select.Item>
        </Select>,
      );

      await user.click(screen.getByRole('combobox', { name: '과일' }));
      await user.click(screen.getByRole('option', { name: '바나나' }));

      expect(screen.getByRole('combobox', { name: '과일' })).toHaveTextContent('사과');
    });

    it('변경된 value에 해당하는 콘텐츠를 표시한다', () => {
      const example = (value: string | null) => (
        <Select aria-label="과일" value={value} placeholder="선택하세요">
          <Select.Item value="apple">사과</Select.Item>
          <Select.Item value="banana">바나나</Select.Item>
        </Select>
      );
      const { rerender } = render(example('apple'));

      rerender(example('banana'));

      expect(screen.getByRole('combobox', { name: '과일' })).toHaveTextContent('바나나');
    });

    it('value가 null이면 placeholder를 표시한다', () => {
      render(
        <Select aria-label="과일" value={null} placeholder="선택하세요">
          <Select.Item value="apple">사과</Select.Item>
        </Select>,
      );

      expect(screen.getByRole('combobox', { name: '과일' })).toHaveTextContent('선택하세요');
    });
  });

  describe('선택 동작', () => {
    it('옵션이 변경되면 onValueChange를 호출한다', async () => {
      const user = userEvent.setup();
      const onValueChange = jest.fn();

      render(
        <Select aria-label="과일" value="apple" onValueChange={onValueChange}>
          <Select.Item value="apple">사과</Select.Item>
          <Select.Item value="banana">바나나</Select.Item>
        </Select>,
      );

      await user.click(screen.getByRole('combobox', { name: '과일' }));
      await user.click(screen.getByRole('option', { name: '바나나' }));

      expect(onValueChange).toHaveBeenCalledWith('banana');
    });

    it('옵션을 선택하면 목록을 닫는다', async () => {
      const user = userEvent.setup();

      render(
        <Select aria-label="과일">
          <Select.Item value="apple">사과</Select.Item>
        </Select>,
      );

      await user.click(screen.getByRole('combobox', { name: '과일' }));
      await user.click(screen.getByRole('option', { name: '사과' }));

      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });

    it('이미 선택한 옵션을 다시 선택하면 변경 이벤트를 호출하지 않는다', async () => {
      const user = userEvent.setup();
      const onValueChange = jest.fn();

      render(
        <Select aria-label="과일" defaultValue="apple" onValueChange={onValueChange}>
          <Select.Item value="apple">사과</Select.Item>
        </Select>,
      );

      await user.click(screen.getByRole('combobox', { name: '과일' }));
      await user.click(screen.getByRole('option', { name: '사과' }));

      expect(onValueChange).not.toHaveBeenCalled();
    });
  });

  describe('비활성 상태', () => {
    it('비활성 옵션을 클릭해도 선택값을 변경하지 않는다', async () => {
      const user = userEvent.setup();
      const onValueChange = jest.fn();

      render(
        <Select aria-label="과일" defaultValue="apple" onValueChange={onValueChange}>
          <Select.Item value="apple">사과</Select.Item>
          <Select.Item value="banana" disabled>
            바나나
          </Select.Item>
        </Select>,
      );

      const trigger = screen.getByRole('combobox', { name: '과일' });
      await user.click(trigger);
      await user.click(screen.getByRole('option', { name: '바나나' }));

      expect(trigger).toHaveTextContent('사과');
      expect(onValueChange).not.toHaveBeenCalled();
    });

    it('disabled이면 포커스하거나 목록을 열 수 없다', async () => {
      const user = userEvent.setup();

      render(
        <Select aria-label="과일" disabled>
          <Select.Item value="apple">사과</Select.Item>
        </Select>,
      );

      const trigger = screen.getByRole('combobox', { name: '과일' });
      await user.click(trigger);

      expect(trigger).toBeDisabled();
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });
  });

  describe('폼 연동', () => {
    it('폼 제출 시 선택값을 포함하고 비활성 Select는 제외한다', async () => {
      const user = userEvent.setup();
      const onSubmit = jest.fn();

      render(
        <form
          onSubmit={(event) => {
            event.preventDefault();
            onSubmit(Object.fromEntries(new FormData(event.currentTarget)));
          }}
        >
          <Select aria-label="과일" name="fruit" defaultValue="apple">
            <Select.Item value="apple">사과</Select.Item>
            <Select.Item value="banana">바나나</Select.Item>
          </Select>
          <Select aria-label="비활성 과일" name="disabledFruit" defaultValue="apple" disabled>
            <Select.Item value="apple">사과</Select.Item>
          </Select>
          <button type="submit">제출</button>
        </form>,
      );

      await user.click(screen.getByRole('combobox', { name: '과일' }));
      await user.click(screen.getByRole('option', { name: '바나나' }));
      await user.click(screen.getByRole('button', { name: '제출' }));

      expect(onSubmit).toHaveBeenCalledWith({ fruit: 'banana' });
    });
  });

  describe('ref', () => {
    it('ref로 트리거에 포커스할 수 있다', async () => {
      const user = userEvent.setup();

      function FocusableSelect() {
        const ref = useRef<HTMLButtonElement>(null);
        const [value, setValue] = useState<string | null>(null);

        return (
          <>
            <Select ref={ref} aria-label="과일" value={value} onValueChange={setValue}>
              <Select.Item value="apple">사과</Select.Item>
            </Select>
            <button onClick={() => ref.current?.focus()}>선택으로 이동</button>
          </>
        );
      }

      render(<FocusableSelect />);
      await user.click(screen.getByRole('button', { name: '선택으로 이동' }));

      expect(screen.getByRole('combobox', { name: '과일' })).toHaveFocus();
    });
  });
});
