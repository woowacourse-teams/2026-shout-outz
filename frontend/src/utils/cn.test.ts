import { cn } from '@/utils/cn';

describe('cn', () => {
  it('조건부 클래스를 결합한다', () => {
    const classes = (hidden: boolean) => cn('rounded-lg', hidden && 'hidden', 'text-sm');

    expect(classes(false)).toBe('rounded-lg text-sm');
    expect(classes(true)).toBe('rounded-lg hidden text-sm');
  });

  it('충돌하는 Tailwind 클래스는 뒤에 온 것이 남는다', () => {
    expect(cn('h-11', 'h-12')).toBe('h-12');
  });

  it('브레이크포인트가 다르면 충돌로 보지 않고 모두 남긴다', () => {
    expect(cn('h-10', 'md:h-12')).toBe('h-10 md:h-12');
  });
});
