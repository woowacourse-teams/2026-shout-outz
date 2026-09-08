import { render, screen } from '@testing-library/react';

import { Avatar } from '@/components/Avatar';

const AVATAR_URL = 'https://avatars.githubusercontent.com/u/1';

const classesOf = (element: HTMLElement) => element.className.split(' ');
const sizeClassesOf = (element: HTMLElement) =>
  classesOf(element).filter((className) => /^size-/.test(className));

describe('Avatar', () => {
  it('src를 넘기면 이미지로 렌더한다', () => {
    render(<Avatar src={AVATAR_URL} alt="정우진" />);

    expect(screen.getByRole('img', { name: '정우진' })).toHaveAttribute('src', AVATAR_URL);
  });

  it('src가 없으면 img 대신 div로 렌더해 깨진 이미지와 alt 텍스트를 노출하지 않는다', () => {
    render(<Avatar alt="정우진" />);

    const avatar = screen.getByRole('img', { name: '정우진' });

    expect(avatar.tagName).toBe('DIV');
    expect(avatar).toBeEmptyDOMElement();
  });

  it('src가 빈 문자열이어도 div로 렌더한다', () => {
    render(<Avatar src="" alt="정우진" />);

    const avatar = screen.getByRole('img', { name: '정우진' });

    expect(avatar.tagName).toBe('DIV');
    expect(avatar).not.toHaveAttribute('src');
  });

  it('alt를 빈 문자열로 넘기면 접근성 트리에서 무시된다', () => {
    render(
      <>
        <Avatar src={AVATAR_URL} alt="" />
        <span>정우진</span>
      </>,
    );

    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('size가 다르면 서로 다른 크기 클래스를 갖는다', () => {
    const sizes = (['xs', 'sm', 'md', 'lg'] as const).map((size) => {
      const { unmount } = render(<Avatar size={size} alt="정우진" />);
      const found = sizeClassesOf(screen.getByRole('img', { name: '정우진' }));
      unmount();

      return found;
    });

    sizes.forEach((size) => expect(size).toHaveLength(1));
    expect(new Set(sizes.map(([size]) => size)).size).toBe(4);
  });

  it('size를 지정하지 않으면 md가 적용된다', () => {
    const { container: 기본 } = render(<Avatar src={AVATAR_URL} alt="정우진" />);
    const { container: 명시 } = render(<Avatar src={AVATAR_URL} size="md" alt="정우진" />);

    expect(기본.querySelector('img')!.className).toBe(명시.querySelector('img')!.className);
  });

  it('flex 안에서 찌그러지지 않도록 shrink-0을 갖는다', () => {
    render(<Avatar src={AVATAR_URL} alt="정우진" />);

    expect(screen.getByRole('img', { name: '정우진' })).toHaveClass('shrink-0');
  });

  it('className으로 size가 정한 크기를 덮어쓸 수 있다', () => {
    render(<Avatar size="md" alt="정우진" className="size-18" />);

    expect(sizeClassesOf(screen.getByRole('img', { name: '정우진' }))).toEqual(['size-18']);
  });

  it('wrapper 없이 엘리먼트 하나만 렌더한다', () => {
    const { container } = render(<Avatar src={AVATAR_URL} alt="정우진" />);

    expect(container.childElementCount).toBe(1);
    expect(container.firstElementChild?.tagName).toBe('IMG');
  });

  it('src와 alt가 모두 없으면 접근성 트리에 노출되지 않는다', () => {
    const { container } = render(<Avatar alt="" />);

    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(container.firstElementChild).not.toHaveAttribute('aria-label');
  });
});
