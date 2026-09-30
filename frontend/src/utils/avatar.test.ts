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

  it('한글 이름이 모든 색 갈래를 쓴다', () => {
    // 코드포인트를 더하기만 하면 한글에서 두 색으로 뭉쳤다. 한글 음절은
    // 0xAC00 + 초성*588 + 중성*28 + 종성이고 세 값이 모두 4의 배수라,
    // 4로 나눈 나머지에 종성만 남기 때문이다. "갈리기만 하면 된다"로는 이걸 못 잡는다.
    const names = [
      '정우진',
      '김도현',
      '이지민',
      '황호익',
      '박성호',
      '최민서',
      '고제성',
      '한소희',
      '강민수',
      '윤서아',
      '임채원',
      '신동현',
      '조하늘',
      '배수지',
      '장원영',
      '문지후',
      '서준서',
      '오세훈',
      '권나라',
      '안지호',
    ];
    const tones = new Set(names.map(getAvatarTone));

    expect(tones.size).toBe(AVATAR_TONES.length);
  });

  it('종성만 다른 이름도 같은 색으로 몰리지 않는다', () => {
    // 가·간·갈·감·개·고·구는 코드포인트를 4로 나눈 나머지가 모두 0이다.
    const names = ['가', '간', '갈', '감', '개', '고', '구'];
    const tones = new Set(names.map(getAvatarTone));

    expect(tones.size).toBeGreaterThan(2);
  });
});
