import { formatDotDate, formatRelativeTime } from '@/utils/date';

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

describe('formatRelativeTime', () => {
  const NOW = new Date('2026-09-15T12:00:00+09:00');

  it('1분 미만이면 방금 전이다', () => {
    expect(formatRelativeTime('2026-09-15T12:00:00+09:00', NOW)).toBe('방금 전');
    expect(formatRelativeTime('2026-09-15T11:59:01+09:00', NOW)).toBe('방금 전');
  });

  it('1시간 미만이면 분 단위로 내림해 보여준다', () => {
    expect(formatRelativeTime('2026-09-15T11:59:00+09:00', NOW)).toBe('1분 전');
    expect(formatRelativeTime('2026-09-15T11:00:01+09:00', NOW)).toBe('59분 전');
  });

  it('1일 미만이면 시간 단위로 내림해 보여준다', () => {
    expect(formatRelativeTime('2026-09-15T11:00:00+09:00', NOW)).toBe('1시간 전');
    expect(formatRelativeTime('2026-09-14T12:00:01+09:00', NOW)).toBe('23시간 전');
  });

  it('7일 미만이면 일 단위로 내림해 보여준다', () => {
    expect(formatRelativeTime('2026-09-14T12:00:00+09:00', NOW)).toBe('1일 전');
    expect(formatRelativeTime('2026-09-08T12:00:01+09:00', NOW)).toBe('6일 전');
  });

  it('7일 이상 지나면 점으로 구분한 날짜로 보여준다', () => {
    expect(formatRelativeTime('2026-09-08T12:00:00+09:00', NOW)).toBe('2026.09.08');
  });

  it('오프셋 표기가 달라도 같은 순간이면 같은 결과다', () => {
    expect(formatRelativeTime('2026-09-15T01:00:00Z', NOW)).toBe('2시간 전');
  });

  it('서버와 기기의 시계 차이로 미래 시각이 오면 방금 전으로 보여준다', () => {
    expect(formatRelativeTime('2026-09-15T12:03:00+09:00', NOW)).toBe('방금 전');
  });

  it('ISO 날짜로 시작하지 않는 값은 던진다', () => {
    expect(() => formatRelativeTime('2시간 전', NOW)).toThrow();
  });
});
