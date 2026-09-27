import { fireEvent, render, screen } from '@testing-library/react';

import { Avatar } from '@/components/Avatar';

const AVATAR_URL = 'https://avatars.githubusercontent.com/u/1';
const OTHER_AVATAR_URL = 'https://avatars.githubusercontent.com/u/2';

const classesOf = (element: Element) => element.className.split(' ');
const sizeClassesOf = (element: Element) =>
  classesOf(element).filter((className) => /^size-/.test(className));

describe('Avatar', () => {
  describe('이미지 표시', () => {
    it('src를 넘기면 이미지로 렌더한다', () => {
      render(<Avatar src={AVATAR_URL} alt="정우진" />);

      expect(screen.getByRole('img', { name: '정우진' })).toHaveAttribute('src', AVATAR_URL);
    });

    it('src가 없으면 이미지를 렌더하지 않는다', () => {
      const { container } = render(<Avatar alt="정우진" />);

      expect(container.querySelector('img')).not.toBeInTheDocument();
      expect(screen.getByRole('img', { name: '정우진' })).toBeEmptyDOMElement();
    });

    it('src가 빈 문자열이어도 이미지를 렌더하지 않는다', () => {
      const { container } = render(<Avatar src="" alt="정우진" />);

      expect(container.querySelector('img')).not.toBeInTheDocument();
    });

    it('이미지 로드에 실패하면 이미지를 내리고 빈 원만 남긴다', () => {
      const { container } = render(<Avatar src={AVATAR_URL} alt="정우진" />);

      fireEvent.error(screen.getByRole('img', { name: '정우진' }));

      expect(container.querySelector('img')).not.toBeInTheDocument();
      expect(screen.getByRole('img', { name: '정우진' })).toBeEmptyDOMElement();
    });

    it('로드에 실패한 뒤 src가 바뀌면 다시 이미지를 시도한다', () => {
      const { container, rerender } = render(<Avatar src={AVATAR_URL} alt="정우진" />);

      fireEvent.error(screen.getByRole('img', { name: '정우진' }));
      rerender(<Avatar src={OTHER_AVATAR_URL} alt="정우진" />);

      expect(container.querySelector('img')).toHaveAttribute('src', OTHER_AVATAR_URL);
    });

    it('loading을 내부 이미지에 전달한다', () => {
      render(<Avatar src={AVATAR_URL} alt="정우진" loading="lazy" />);

      expect(screen.getByRole('img', { name: '정우진' })).toHaveAttribute('loading', 'lazy');
    });
  });

  describe('접근성', () => {
    it('이미지가 없어도 alt로 접근 가능한 이름을 갖는다', () => {
      render(<Avatar alt="정우진" />);

      expect(screen.getByRole('img', { name: '정우진' })).toBeInTheDocument();
    });

    it('alt가 비어 있으면 접근성 트리에 노출되지 않는다', () => {
      const { container } = render(<Avatar alt="" />);

      expect(screen.queryByRole('img')).not.toBeInTheDocument();
      expect(container.firstElementChild).not.toHaveAttribute('aria-label');
    });
  });

  describe('스타일', () => {
    it('size가 다르면 서로 다른 크기 클래스를 갖는다', () => {
      const sizes = (['xs', 'sm', 'md', 'lg'] as const).map((size) => {
        const { container, unmount } = render(<Avatar size={size} alt="정우진" />);
        const found = sizeClassesOf(container.firstElementChild!);
        unmount();

        return found;
      });

      sizes.forEach((size) => expect(size).toHaveLength(1));
      expect(new Set(sizes.map(([size]) => size)).size).toBe(4);
    });

    it('size를 지정하지 않으면 md가 적용된다', () => {
      const { container: 기본 } = render(<Avatar alt="정우진" />);
      const { container: 명시 } = render(<Avatar size="md" alt="정우진" />);

      expect(기본.firstElementChild!.className).toBe(명시.firstElementChild!.className);
    });

    it('flex 안에서 찌그러지지 않도록 shrink-0을 갖는다', () => {
      const { container } = render(<Avatar src={AVATAR_URL} alt="정우진" />);

      expect(container.firstElementChild).toHaveClass('shrink-0');
    });

    it('className으로 size가 정한 크기를 덮어쓸 수 있다', () => {
      const { container } = render(<Avatar size="md" alt="정우진" className="size-18" />);

      expect(sizeClassesOf(container.firstElementChild!)).toEqual(['size-18']);
    });
  });
});

describe('Avatar 기본 프로필', () => {
  it('이미지가 없으면 이름의 첫 글자를 보여준다', () => {
    render(<Avatar name="정우진" alt="정우진" />);

    expect(screen.getByRole('img', { name: '정우진' })).toHaveTextContent('정');
  });

  it('이미지가 있으면 첫 글자를 보여주지 않는다', () => {
    render(<Avatar src={AVATAR_URL} name="정우진" alt="정우진" />);

    expect(screen.getByRole('img', { name: '정우진' })).toHaveAttribute('src', AVATAR_URL);
    expect(screen.queryByText('정')).not.toBeInTheDocument();
  });

  it('이미지 로드에 실패하면 기본 프로필로 돌아간다', () => {
    const { container } = render(<Avatar src={AVATAR_URL} name="정우진" alt="정우진" />);

    fireEvent.error(container.querySelector('img')!);

    expect(container.querySelector('img')).not.toBeInTheDocument();
    expect(screen.getByRole('img', { name: '정우진' })).toHaveTextContent('정');
  });

  it('이름이 없으면 빈 원을 그린다', () => {
    render(<Avatar alt="프로필" />);

    expect(screen.getByRole('img', { name: '프로필' })).toBeEmptyDOMElement();
  });

  it('같은 이름은 늘 같은 색으로 그린다', () => {
    const { container: first } = render(<Avatar name="정우진" alt="" />);
    const { container: second } = render(<Avatar name="정우진" alt="" />);

    expect(first.firstElementChild?.className).toBe(second.firstElementChild?.className);
  });

  it('이름이 다르면 색이 갈린다', () => {
    const { container: woojin } = render(<Avatar name="정우진" alt="" />);
    const { container: dohyun } = render(<Avatar name="김도현" alt="" />);

    expect(woojin.firstElementChild?.className).not.toBe(dohyun.firstElementChild?.className);
  });

  it('첫 글자는 이름 옆에서 중복으로 읽히지 않는다', () => {
    const { container } = render(<Avatar name="정우진" alt="" />);

    expect(container.querySelector('span')).toHaveAttribute('aria-hidden', 'true');
  });

  it('alt가 빈 문자열이면 img에 alt 속성이 빈 값으로 남는다', () => {
    const { container } = render(<Avatar src={AVATAR_URL} name="정우진" alt="" />);
    const image = container.querySelector('img')!;

    // alt 속성이 없는 것과 빈 alt는 다르다. 없으면 스크린리더가 파일명을 읽고,
    // 비어 있으면 장식으로 보고 건너뛴다.
    expect(image.hasAttribute('alt')).toBe(true);
    expect(image.getAttribute('alt')).toBe('');
  });

  it('src가 null이면 기본 프로필로 떨어진다', () => {
    // 서버가 사진 없는 유저에게 주는 값. 호출부가 undefined로 바꾸지 않아도 된다.
    render(<Avatar src={null} name="정우진" alt="정우진" />);

    expect(screen.getByRole('img', { name: '정우진' })).toHaveTextContent('정');
  });

  it('alt가 빈 문자열이면 기본 프로필도 이름을 읽지 않는다', () => {
    const { container } = render(<Avatar name="정우진" alt="" />);
    const box = container.firstElementChild!;

    expect(box).not.toHaveAttribute('role');
    expect(box).not.toHaveAttribute('aria-label');
  });
});
