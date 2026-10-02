import { useRef, useState } from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Input } from '@/components/Input';

const getHeightClasses = (element: HTMLElement) =>
  element.className.split(' ').filter((className) => /^h-/.test(className));

describe('Input', () => {
  it('사용자가 입력한 값이 화면에 반영된다', async () => {
    const user = userEvent.setup();

    render(<Input aria-label="프로젝트 이름" />);

    const input = screen.getByRole('textbox', { name: '프로젝트 이름' });
    await user.type(input, '모아모아');

    expect(input).toHaveValue('모아모아');
  });

  it('제어 컴포넌트로 사용할 수 있다', async () => {
    const user = userEvent.setup();

    function ControlledInput() {
      const [value, setValue] = useState('');

      return (
        <Input
          aria-label="검색어"
          value={value}
          onChange={(event) => setValue(event.target.value)}
        />
      );
    }

    render(<ControlledInput />);

    const input = screen.getByRole('textbox', { name: '검색어' });
    await user.type(input, '블록체인');

    expect(input).toHaveValue('블록체인');
  });

  it('readOnly이면 값이 보이지만 수정되지 않는다', async () => {
    const user = userEvent.setup();

    render(<Input aria-label="크루 이름" readOnly defaultValue="정우진" />);

    const input = screen.getByRole('textbox', { name: '크루 이름' });
    await user.type(input, '김민지');

    expect(input).toHaveValue('정우진');
  });

  it('label과 연결하면 라벨 텍스트로 찾을 수 있다', () => {
    render(
      <>
        <label htmlFor="project-name">프로젝트 이름</label>
        <Input id="project-name" />
      </>,
    );

    expect(screen.getByLabelText('프로젝트 이름')).toBeInTheDocument();
  });

  it('aria-invalid를 전달한다', () => {
    render(<Input aria-label="GitHub 레포지토리 URL" aria-invalid />);

    expect(screen.getByRole('textbox', { name: 'GitHub 레포지토리 URL' })).toHaveAttribute(
      'aria-invalid',
      'true',
    );
  });

  it('className으로 size가 정한 높이를 덮어쓸 수 있다', () => {
    render(<Input aria-label="검색어" size="md" className="h-12" />);

    expect(getHeightClasses(screen.getByRole('textbox', { name: '검색어' }))).toEqual(['h-12']);
  });

  it('size가 다르면 서로 다른 높이 클래스를 갖는다', () => {
    const { rerender } = render(<Input aria-label="검색어" size="sm" />);
    const small = getHeightClasses(screen.getByRole('textbox', { name: '검색어' }));

    rerender(<Input aria-label="검색어" size="lg" />);
    const large = getHeightClasses(screen.getByRole('textbox', { name: '검색어' }));

    expect(small).toHaveLength(1);
    expect(large).toHaveLength(1);
    expect(small).not.toEqual(large);
  });

  it('ref로 input 엘리먼트에 접근할 수 있다', async () => {
    const user = userEvent.setup();

    function FocusableInput() {
      const inputRef = useRef<HTMLInputElement>(null);

      return (
        <>
          <Input aria-label="검색어" ref={inputRef} />
          <button onClick={() => inputRef.current?.focus()}>검색창으로 이동</button>
        </>
      );
    }

    render(<FocusableInput />);

    await user.click(screen.getByRole('button', { name: '검색창으로 이동' }));

    expect(screen.getByRole('textbox', { name: '검색어' })).toHaveFocus();
  });
});
