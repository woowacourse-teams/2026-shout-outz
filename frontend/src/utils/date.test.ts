import { formatDotDate } from '@/utils/date';

describe('formatDotDate', () => {
  it('ISO date-time의 날짜 부분을 점으로 구분해 보여준다', () => {
    expect(formatDotDate('2026-08-25T10:00:00+09:00')).toBe('2026.08.25');
  });

  it('한 자리 월·일도 서버가 보낸 0 채움을 유지한다', () => {
    expect(formatDotDate('2026-07-05T10:00:00+09:00')).toBe('2026.07.05');
  });

  it('오프셋을 해석하지 않고 서버가 보낸 날짜를 그대로 쓴다', () => {
    // 절대 시점으로 환산하면 UTC 기준 2026-08-24이지만, 서버가 보낸 날짜는 8월 25일이다.
    expect(formatDotDate('2026-08-25T00:30:00+09:00')).toBe('2026.08.25');
    expect(formatDotDate('2026-08-25T23:30:00Z')).toBe('2026.08.25');
  });

  it('시각 없이 날짜만 와도 동작한다', () => {
    expect(formatDotDate('2026-08-25')).toBe('2026.08.25');
  });

  it('ISO 날짜로 시작하지 않는 값은 던진다', () => {
    expect(() => formatDotDate('내일')).toThrow();
    expect(() => formatDotDate('2026/08/25')).toThrow();
  });
});
