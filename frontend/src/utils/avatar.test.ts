import { AVATAR_TONES, AVATAR_TONE_CLASSES, getAvatarInitial, getAvatarTone } from '@/utils/avatar';

describe('getAvatarInitial', () => {
  it('이름의 첫 글자를 쓴다', () => {
    expect(getAvatarInitial('정우진')).toBe('정');
  });

  it('영문은 대문자로 보여준다', () => {
    expect(getAvatarInitial('jaeki')).toBe('J');
  });

  it('앞뒤 공백은 무시한다', () => {
    expect(getAvatarInitial('  두리 ')).toBe('두');
  });

  it('코드 포인트가 둘 이상인 글자도 쪼개지 않는다', () => {
    expect(getAvatarInitial('😀웃음')).toBe('😀');
  });

  it('이름이 비면 빈 문자열이다', () => {
    expect(getAvatarInitial('')).toBe('');
    expect(getAvatarInitial('   ')).toBe('');
  });
});

describe('getAvatarTone', () => {
  it('같은 이름은 늘 같은 색이다', () => {
    expect(getAvatarTone('정우진')).toBe(getAvatarTone('정우진'));
  });

  it('앞뒤 공백이 달라도 같은 색이다', () => {
    expect(getAvatarTone(' 정우진 ')).toBe(getAvatarTone('정우진'));
  });

  it('팔레트 안의 색만 돌려준다', () => {
    const names = ['정우진', '김도현', '이지민', '황호익', 'jaeki', '두리', '', '😀'];

    for (const name of names) {
      expect(AVATAR_TONES).toContain(getAvatarTone(name));
      expect(AVATAR_TONE_CLASSES[getAvatarTone(name)]).toBeTruthy();
    }
  });

  it('이름이 다르면 색도 갈린다', () => {
    const names = ['정우진', '김도현', '이지민', '황호익', '박성호', '최민서'];
    const tones = new Set(names.map(getAvatarTone));

    expect(tones.size).toBeGreaterThan(1);
  });
});
