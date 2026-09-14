import { formatDotDate } from '@/utils/date';

describe('formatDotDate', () => {
  it('ISO date-time을 점으로 구분한 날짜로 보여준다', () => {
    expect(formatDotDate('2026-08-25T10:00:00+09:00')).toBe('2026.08.25');
  });

  it('한 자리 월·일을 0으로 채운다', () => {
    expect(formatDotDate('2026-07-05T10:00:00+09:00')).toBe('2026.07.05');
  });

  describe('표시 시간대', () => {
    it('UTC로 와도 한국 날짜로 보여준다', () => {
      expect(formatDotDate('2026-08-25T23:30:00Z')).toBe('2026.08.26');
    });

    it('같은 순간이면 오프셋 표기가 달라도 같은 날짜다', () => {
      expect(formatDotDate('2026-08-25T00:30:00+09:00')).toBe('2026.08.25');
      expect(formatDotDate('2026-08-24T15:30:00Z')).toBe('2026.08.25');
    });
  });

  it('시각 없이 날짜만 와도 동작한다', () => {
    expect(formatDotDate('2026-08-25')).toBe('2026.08.25');
  });

  it('ISO 날짜로 시작하지 않는 값은 던진다', () => {
    expect(() => formatDotDate('내일')).toThrow();
    expect(() => formatDotDate('2026/08/25')).toThrow();
  });
});
