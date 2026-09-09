import { render, screen } from '@testing-library/react';

import { Badge, type BadgeTone, type BadgeVariant } from '@/components/Badge';

const VARIANTS: BadgeVariant[] = ['soft', 'solid'];
const TONES: BadgeTone[] = ['gray', 'primary', 'green'];
const COMBINATIONS = VARIANTS.flatMap((variant) => TONES.map((tone) => ({ variant, tone })));

const classesOf = (element: HTMLElement) => element.className.split(' ');
const classMatching = (element: HTMLElement, matcher: RegExp) =>
  classesOf(element).filter((className) => matcher.test(className));

const renderBadge = (props: { variant?: BadgeVariant; tone?: BadgeTone }, matcher: RegExp) => {
  const { unmount } = render(<Badge {...props}>이벤트</Badge>);
  const found = classMatching(screen.getByText('이벤트'), matcher);
  unmount();

  return found;
};

describe('Badge', () => {
  describe('variant와 tone', () => {
    it('여섯 조합이 서로 다른 배경색을 갖는다', () => {
      const backgrounds = COMBINATIONS.map((combination) => renderBadge(combination, /^bg-/));

      backgrounds.forEach((background) => expect(background).toHaveLength(1));
      expect(new Set(backgrounds.map(([background]) => background)).size).toBe(COMBINATIONS.length);
    });

    it('solid는 tone과 무관하게 흰 글자를 쓴다', () => {
      const colors = TONES.map((tone) => renderBadge({ variant: 'solid', tone }, /^text-(?!xs$)/));

      colors.forEach((color) => expect(color).toEqual(['text-white']));
    });

    it('soft는 tone마다 다른 글자색을 쓴다', () => {
      const colors = TONES.map((tone) => renderBadge({ variant: 'soft', tone }, /^text-(?!xs$)/));

      colors.forEach((color) => expect(color).toHaveLength(1));
      expect(new Set(colors.map(([color]) => color)).size).toBe(TONES.length);
      expect(colors.flat()).not.toContain('text-white');
    });
  });

  describe('굵기', () => {
    it('gray만 보통 굵기이고 유채색은 굵게 표시된다', () => {
      const weightOf = (tone: BadgeTone, variant: BadgeVariant) =>
        renderBadge({ variant, tone }, /^font-/);

      VARIANTS.forEach((variant) => {
        expect(weightOf('gray', variant)).toEqual(['font-normal']);
        expect(weightOf('primary', variant)).toEqual(['font-bold']);
        expect(weightOf('green', variant)).toEqual(['font-bold']);
      });
    });
  });

  describe('기본값', () => {
    it('variant와 tone을 지정하지 않으면 soft·gray가 적용된다', () => {
      const { container: 기본 } = render(<Badge>React</Badge>);
      const { container: 명시 } = render(
        <Badge variant="soft" tone="gray">
          React
        </Badge>,
      );

      expect(기본.querySelector('span')!.className).toBe(명시.querySelector('span')!.className);
    });
  });

  describe('className', () => {
    it('className으로 radius를 덮어쓸 수 있다', () => {
      render(<Badge className="rounded-full">금주의 추천 프로젝트</Badge>);

      const radii = classMatching(screen.getByText('금주의 추천 프로젝트'), /^rounded/);

      expect(radii).toEqual(['rounded-full']);
    });

    it('className으로 tone이 정한 배경을 덮어쓸 수 있다', () => {
      render(<Badge className="bg-white/20">금주의 추천 프로젝트</Badge>);

      const backgrounds = classMatching(screen.getByText('금주의 추천 프로젝트'), /^bg-/);

      expect(backgrounds).toEqual(['bg-white/20']);
    });
  });

  describe('구조', () => {
    it('wrapper 없이 span 엘리먼트 하나만 렌더한다', () => {
      const { container } = render(<Badge>공지사항</Badge>);

      expect(container.childElementCount).toBe(1);
      expect(container.firstElementChild?.tagName).toBe('SPAN');
    });

    it('상호작용이 없으므로 button role을 갖지 않는다', () => {
      render(<Badge>작성자</Badge>);

      expect(screen.getByText('작성자')).toBeInTheDocument();
      expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });
  });
});
