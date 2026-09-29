/**
 * 프로필 이미지가 없을 때 대신 보여줄 기본 아바타의 글자와 색.
 *
 * 색은 이름에서 계산한다. 같은 이름은 어디서 보든 늘 같은 색이라, 목록과 상세를 오가도 사람이
 * 바뀌지 않은 것처럼 보인다. 저장하거나 서버에서 받아올 값이 아니다.
 */

/**
 * 색 갈래. 색상 다섯 가지 × 원 모양 두 가지다.
 *
 * 색이 의미를 나르지 않고 사람을 구분하기만 하므로, 컬러 토큰 규칙상 "색상 자체를 표현하는 경우"에
 * 해당해 `primary` 별칭 대신 색 이름 토큰을 쓴다.
 *
 * 색상만으로는 네 가지뿐이라 같은 화면에서 자주 겹쳤다. 단계(50/100/200…)를 섞는 방법은
 * 옅은 쪽 끝의 대비가 1.03~1.11이라 눈으로 구분되지 않아, 대신 배경과 글자를 뒤집은
 * `solid`를 더해 가짓수를 두 배로 늘렸다.
 */
export const AVATAR_TONES = [
  'blue-soft',
  'green-soft',
  'yellow-soft',
  'red-soft',
  'gray-soft',
  'blue-solid',
  'green-solid',
  'yellow-solid',
  'red-solid',
  'gray-solid',
] as const;

export type AvatarTone = (typeof AVATAR_TONES)[number];

/**
 * `soft`는 옅은 배경에 진한 글자, `solid`는 그 반대다.
 *
 * 800 단계를 쓰는 이유는 대비다. 기존 `text-*-600`은 라이트 모드에서 green 3.52,
 * yellow 2.20으로 WCAG AA(4.5)에 못 미쳤다. 800으로 올리면 라이트·다크 모두 5.01 이상이다.
 */
export const AVATAR_TONE_CLASSES: Record<AvatarTone, string> = {
  'blue-soft': 'bg-blue-50 text-blue-800',
  'green-soft': 'bg-green-50 text-green-800',
  'yellow-soft': 'bg-yellow-50 text-yellow-800',
  'red-soft': 'bg-red-50 text-red-800',
  'gray-soft': 'bg-gray-50 text-gray-800',
  'blue-solid': 'bg-blue-800 text-blue-50',
  'green-solid': 'bg-green-800 text-green-50',
  'yellow-solid': 'bg-yellow-800 text-yellow-50',
  'red-solid': 'bg-red-800 text-red-50',
  'gray-solid': 'bg-gray-800 text-gray-50',
};

/**
 * 이름을 색 하나로 접는다.
 *
 * 코드 포인트 단위로 훑어 한글·영문·이모지를 가리지 않는다. 같은 이름은 어디서 보든 늘 같은
 * 색이어야 하므로 난수를 쓰지 않는다.
 */
export function getAvatarTone(name: string): AvatarTone {
  let hash = 0;
  for (const char of name.trim()) {
    hash = (Math.imul(hash, 31) + (char.codePointAt(0) ?? 0)) | 0;
  }

  // 여기서 상위 비트를 하위로 접지 않으면 한글 이름의 색이 뭉친다.
  // 한글 음절은 0xAC00 + 초성*588 + 중성*28 + 종성인데 세 값이 모두 4의 배수라,
  // 4로 나눈 나머지에는 종성만 남고 초성·중성이 통째로 사라진다.
  hash ^= hash >>> 15;
  hash = Math.imul(hash, 0x2545f491);
  hash ^= hash >>> 13;

  return AVATAR_TONES[(hash >>> 0) % AVATAR_TONES.length] ?? AVATAR_TONES[0];
}

/**
 * 이름에서 보여줄 글자 하나.
 *
 * 한글은 첫 글자, 영문은 대문자로 바꿔 보여준다. 이모지처럼 코드 포인트가 둘 이상인 글자도
 * 쪼개지지 않게 코드 포인트 단위로 자른다. 이름이 비면 빈 문자열이라 호출부가 글자 없는 원을 그린다.
 */
export function getAvatarInitial(name: string): string {
  const [first] = Array.from(name.trim());

  return first ? first.toUpperCase() : '';
}
