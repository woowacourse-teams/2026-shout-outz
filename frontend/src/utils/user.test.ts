import { formatCrewName, formatCrewRole } from '@/utils/user';

describe('formatCrewRole', () => {
  it.each([
    ['BACKEND', '6기 백엔드'],
    ['FRONTEND', '6기 프론트엔드'],
  ])('기수와 트랙 %s를 소속 문구로 만든다', (track, expected) => {
    expect(formatCrewRole(6, track)).toBe(expected);
  });

  it('아는 것만 보여준다', () => {
    expect(formatCrewRole(6, null)).toBe('6기');
    expect(formatCrewRole(null, 'BACKEND')).toBe('백엔드');
  });

  it('한글 표기를 모르는 트랙은 빼고 보여준다', () => {
    expect(formatCrewRole(6, 'NOT_A_REAL_TRACK')).toBe('6기');
    expect(formatCrewRole(null, 'NOT_A_REAL_TRACK')).toBeNull();
  });

  it('기수도 트랙도 없으면 표시하지 않는다', () => {
    expect(formatCrewRole(null, null)).toBeNull();
  });
});

describe('formatCrewName', () => {
  it('이름과 소속을 가운뎃점으로 잇는다', () => {
    expect(formatCrewName('황호익', 6, 'BACKEND')).toBe('황호익 · 6기 백엔드');
  });

  it('아는 소속만 붙인다', () => {
    expect(formatCrewName('황호익', 6, null)).toBe('황호익 · 6기');
  });

  it('소속을 알 수 없으면 이름만 보여준다', () => {
    expect(formatCrewName('황호익', null, null)).toBe('황호익');
  });
});
