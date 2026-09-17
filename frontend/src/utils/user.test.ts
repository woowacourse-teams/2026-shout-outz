import { formatCrewRole } from '@/utils/user';

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
