/**
 * 프로필 이미지가 없을 때 대신 보여줄 기본 아바타의 글자와 색.
 *
 * 색은 이름에서 계산한다. 같은 이름은 어디서 보든 늘 같은 색이라, 목록과 상세를 오가도 사람이
 * 바뀌지 않은 것처럼 보인다. 저장하거나 서버에서 받아올 값이 아니다.
 */

/**
 * 색 갈래. 디자인이 이니셜 원에 쓰는 네 가지와 같다(🖥️ 17의 참여 팀원 목록).
 *
 * 여기서 색은 의미를 나르지 않고 사람을 구분하기만 하므로, 컬러 토큰 규칙상
 * "색상 자체를 표현하는 경우"에 해당해 색 이름 토큰을 그대로 쓴다.
 */
export const AVATAR_TONES = ['primary', 'green', 'yellow', 'red'] as const;

export type AvatarTone = (typeof AVATAR_TONES)[number];

/** 배경은 옅게, 글자는 진하게. Badge의 `soft`와 같은 짝이다. */
export const AVATAR_TONE_CLASSES: Record<AvatarTone, string> = {
  primary: 'bg-primary-50 text-primary-600',
  green: 'bg-green-50 text-green-600',
  yellow: 'bg-yellow-50 text-yellow-600',
  red: 'bg-red-50 text-red-600',
};

/**
 * 이름을 색 하나로 접는다.
 *
 * 코드 포인트를 훑어 더하는 것뿐이라 한글·영문·이모지를 가리지 않는다. 고르게 흩어지는 것보다
 * 같은 이름이 늘 같은 색으로 나오는 것이 중요해서 단순하게 둔다.
 */
export function getAvatarTone(name: string): AvatarTone {
  let hash = 0;
  for (const char of name.trim()) {
    hash = (hash + (char.codePointAt(0) ?? 0)) % AVATAR_TONES.length;
  }

  return AVATAR_TONES[hash] ?? AVATAR_TONES[0];
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
