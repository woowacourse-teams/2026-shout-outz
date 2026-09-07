import { useState } from 'react';
import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom/vitest';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Tab } from '@/components/Tab';

afterEach(cleanup);

const items = ['first', 'second'];

function renderTab(value?: string, onChange?: (value: string) => void, order = items) {
  return (
    <Tab value={value} onChange={onChange}>
      {order.map((item) => (
        <Tab.Item key={item} value={item}>
          {item}
        </Tab.Item>
      ))}
    </Tab>
  );
}

function expectSelection(value?: string) {
  expect(screen.queryAllByRole('tab', { selected: true })).toEqual(
    value ? [screen.getByRole('tab', { name: value })] : [],
  );
}

describe('Tab', () => {
  it('variant를 생략하면 underline을 적용한다', () => {
    render(renderTab());

    expect(screen.getByRole('tablist')).toHaveAttribute('data-variant', 'underline');
  });

  it.each(['weak', 'chip'] as const)('%s variant를 적용한다', (variant) => {
    render(
      <Tab variant={variant}>
        <Tab.Item value="first">first</Tab.Item>
      </Tab>,
    );

    expect(screen.getByRole('tablist')).toHaveAttribute('data-variant', variant);
  });

  it('size를 생략하면 md를 적용한다', () => {
    render(renderTab());

    expect(screen.getByRole('tablist')).toHaveAttribute('data-size', 'md');
  });

  it.each(['sm', 'lg'] as const)('%s size를 적용한다', (size) => {
    render(
      <Tab size={size}>
        <Tab.Item value="first">first</Tab.Item>
      </Tab>,
    );

    expect(screen.getByRole('tablist')).toHaveAttribute('data-size', size);
  });

  it('초기 value에 해당하는 항목만 선택 상태로 표시한다', () => {
    render(renderTab('second'));
    expectSelection('second');
  });

  it('value 없이 클릭하면 값을 알리되 선택 상태는 생기지 않는다', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(renderTab(undefined, onChange));
    expectSelection();

    await user.click(screen.getByRole('tab', { name: 'first' }));

    expect(onChange).toHaveBeenCalledExactlyOnceWith('first');
    expectSelection();
  });

  it('다른 항목 클릭 시 해당 value로 onChange를 호출한다', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(renderTab('first', onChange));

    await user.click(screen.getByRole('tab', { name: 'second' }));

    expect(onChange).toHaveBeenCalledExactlyOnceWith('second');
  });

  it('onChange 없이 클릭해도 오류 없이 기존 선택을 유지한다', async () => {
    const user = userEvent.setup();
    render(renderTab('first'));

    await user.click(screen.getByRole('tab', { name: 'second' }));

    expectSelection('first');
  });

  it('부모가 value를 바꾸지 않으면 기존 선택 상태를 유지한다', async () => {
    const user = userEvent.setup();
    render(renderTab('first', vi.fn()));

    await user.click(screen.getByRole('tab', { name: 'second' }));

    expectSelection('first');
  });

  it('부모가 value를 바꾸면 새 항목으로 선택 상태를 변경한다', () => {
    const { rerender } = render(renderTab('first'));

    rerender(renderTab('second'));

    expectSelection('second');
  });

  it('이미 선택된 항목 재클릭 시 onChange를 호출하지 않는다', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(renderTab('first', onChange));

    await user.click(screen.getByRole('tab', { name: 'first' }));

    expect(onChange).not.toHaveBeenCalled();
    expectSelection('first');
  });

  it('존재하지 않는 value를 전달하면 임의 선택하지 않는다', () => {
    render(renderTab('missing'));
    expectSelection();
  });

  it('항목 순서가 바뀌어도 value 기준으로 선택을 유지한다', () => {
    const { rerender } = render(renderTab('second'));

    rerender(renderTab('second', undefined, ['second', 'first']));

    expectSelection('second');
  });

  it('여러 Tab 인스턴스가 서로 영향을 주지 않는다', async () => {
    const user = userEvent.setup();
    const onSecondChange = vi.fn();
    function Example() {
      const [value, setValue] = useState('first');
      return (
        <>
          <section aria-label="첫 번째 목록">{renderTab(value, setValue)}</section>
          <section aria-label="두 번째 목록">{renderTab('first', onSecondChange)}</section>
        </>
      );
    }
    render(<Example />);
    const first = within(screen.getByRole('region', { name: '첫 번째 목록' }));
    const second = within(screen.getByRole('region', { name: '두 번째 목록' }));

    await user.click(first.getByRole('tab', { name: 'second' }));

    expect(first.getAllByRole('tab', { selected: true })).toEqual([
      first.getByRole('tab', { name: 'second' }),
    ]);
    expect(second.getAllByRole('tab', { selected: true })).toEqual([
      second.getByRole('tab', { name: 'first' }),
    ]);
    expect(onSecondChange).not.toHaveBeenCalled();
  });

  it('폼 안에서 클릭해도 제출하지 않는다', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const onSubmit = vi.fn((event) => event.preventDefault());
    render(<form onSubmit={onSubmit}>{renderTab('first', onChange)}</form>);

    await user.click(screen.getByRole('tab', { name: 'second' }));

    expect(onChange).toHaveBeenCalledExactlyOnceWith('second');
    expect(onSubmit).not.toHaveBeenCalled();
  });
});
