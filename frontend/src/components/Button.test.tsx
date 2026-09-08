import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Button, getButtonStyles } from '@/components/Button';

const classesOf = (element: HTMLElement) => element.className.split(' ');
const hasClass = (element: HTMLElement, matcher: RegExp) =>
  classesOf(element).some((className) => matcher.test(className));

describe('Button', () => {
  describe('type 기본값', () => {
    it('form 안에서 기본 Button은 폼을 제출하지 않는다', async () => {
      const user = userEvent.setup();
      const handleSubmit = jest.fn();

      render(
        <form
          onSubmit={(event) => {
            event.preventDefault();
            handleSubmit();
          }}
        >
          <Button>취소</Button>
        </form>,
      );

      await user.click(screen.getByRole('button', { name: '취소' }));

      expect(handleSubmit).not.toHaveBeenCalled();
    });

    it('type="submit"을 주면 폼을 제출한다', async () => {
      const user = userEvent.setup();
      const handleSubmit = jest.fn();

      render(
        <form
          onSubmit={(event) => {
            event.preventDefault();
            handleSubmit();
          }}
        >
          <Button type="submit">피드 등록하기</Button>
        </form>,
      );

      await user.click(screen.getByRole('button', { name: '피드 등록하기' }));

      expect(handleSubmit).toHaveBeenCalledTimes(1);
    });
  });

  describe('variant', () => {
    it.each([
      ['primary', { 배경: true, 테두리: false }],
      ['secondary', { 배경: true, 테두리: true }],
      ['outline', { 배경: false, 테두리: true }],
      ['ghost', { 배경: false, 테두리: false }],
    ] as const)('%s는 배경과 테두리 조합으로 구분된다', (variant, expected) => {
      render(
        <Button variant={variant} size="md">
          버튼
        </Button>,
      );

      const button = screen.getByRole('button', { name: '버튼' });

      expect(hasClass(button, /^bg-/)).toBe(expected.배경);
      expect(hasClass(button, /^border/)).toBe(expected.테두리);
    });
  });

  describe('size', () => {
    it('size가 다르면 서로 다른 높이 클래스를 갖는다', () => {
      const heights = (['sm', 'md', 'lg'] as const).map((size) => {
        const { unmount } = render(<Button size={size}>버튼</Button>);
        const found = classesOf(screen.getByRole('button', { name: '버튼' })).filter((className) =>
          /^h-/.test(className),
        );
        unmount();

        return found;
      });

      heights.forEach((height) => expect(height).toHaveLength(1));
      expect(new Set(heights.map(([height]) => height)).size).toBe(3);
    });
  });

  describe('기본값', () => {
    it('variant와 size를 지정하지 않으면 primary·md가 적용된다', () => {
      const { container: 기본 } = render(<Button>버튼</Button>);
      const { container: 명시 } = render(
        <Button variant="primary" size="md">
          버튼
        </Button>,
      );

      expect(기본.querySelector('button')!.className).toBe(명시.querySelector('button')!.className);
    });
  });

  describe('너비', () => {
    it('너비 클래스를 갖지 않아 기본이 fit-content로 남는다', () => {
      render(<Button>글쓰기</Button>);

      expect(hasClass(screen.getByRole('button', { name: '글쓰기' }), /^w-/)).toBe(false);
    });

    it('className으로 전체폭을 지정할 수 있다', () => {
      render(<Button className="w-full">4개 스택 선택 완료</Button>);

      expect(screen.getByRole('button', { name: '4개 스택 선택 완료' })).toHaveClass('w-full');
    });
  });

  describe('className', () => {
    it('className으로 size가 정한 높이를 덮어쓸 수 있다', () => {
      render(
        <Button size="md" className="h-11">
          취소
        </Button>,
      );

      const heights = classesOf(screen.getByRole('button', { name: '취소' })).filter((className) =>
        /^h-/.test(className),
      );

      expect(heights).toEqual(['h-11']);
    });
  });

  describe('구조', () => {
    it('wrapper 없이 button 엘리먼트 하나만 렌더한다', () => {
      const { container } = render(<Button>글쓰기</Button>);

      expect(container.childElementCount).toBe(1);
      expect(container.firstElementChild?.tagName).toBe('BUTTON');
    });
  });
});

describe('getButtonStyles', () => {
  it('같은 옵션의 Button과 동일한 클래스를 만든다', () => {
    render(
      <Button variant="outline" size="lg">
        메인 홈으로 이동
      </Button>,
    );

    const button = screen.getByRole('button', { name: '메인 홈으로 이동' });

    expect(getButtonStyles({ variant: 'outline', size: 'lg' })).toBe(button.className);
  });

  it('링크에 버튼 스타일을 입힐 수 있다', () => {
    render(
      <a href="https://github.com/login/oauth" className={getButtonStyles({ size: 'lg' })}>
        GitHub 계정으로 시작하기
      </a>,
    );

    const link = screen.getByRole('link', { name: 'GitHub 계정으로 시작하기' });

    expect(hasClass(link, /^h-/)).toBe(true);
  });
});
