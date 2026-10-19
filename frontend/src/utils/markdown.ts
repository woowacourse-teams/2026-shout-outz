/**
 * 마크다운을 미리보기용 한 줄 평문으로 바꾼다.
 *
 * 목록 카드처럼 몇 줄만 잘라 보여주는 곳에서 `##`, `**`, 코드 펜스 같은 기호가 그대로 보이지 않게 한다.
 * 렌더링용이 아니라서 문법을 완벽히 해석하지 않고 흔한 기호만 걷어낸다.
 * - 코드 블록과 이미지는 통째로 뺀다 (미리보기에서 읽을 수 없다)
 * - 링크는 글자만 남긴다: `[블로그](https://…)` → `블로그`
 * - 줄바꿈은 공백 하나로 합친다
 */
export function toPlainText(markdown: string): string {
  return markdown
    .replace(/```[\s\S]*?(```|$)/g, ' ')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, ' ')
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/`([^`]*)`/g, '$1')
    .replace(/^\s{0,3}(#{1,6}|>|[-*+]|\d+\.)\s+/gm, '')
    .replace(/(\*\*|__|~~|\*|_)(\S(?:.*?\S)?)\1/g, '$2')
    .replace(/\s+/g, ' ')
    .trim();
}

const COLOR_STEPS = ['50', '100', '200', '300', '400', '500', '600', '700', '800', '900', '950'];
const ADAPTIVE_HUES = ['blue', 'red', 'yellow', 'green', 'gray'];
const STATIC_HUES = ['blue', 'red', 'yellow', 'green'];

/** 색 이름만 쓴 경우의 기본 단계. 본문 위에서 읽히는 진하기로 고른다. */
const NAMED_COLORS: Record<string, string> = {
  red: 'red-600',
  blue: 'blue-600',
  green: 'green-600',
  yellow: 'yellow-600',
  gray: 'gray-500',
  primary: 'primary-600',
};

/**
 * `color` 속성 값을 컬러 토큰의 CSS 변수로 바꾼다. 토큰이 아닌 값(hex, rgb 등)은 무시한다.
 *
 * 받는 값:
 * - 색 이름: `red`, `primary`
 * - 토큰 이름: `blue-500`, `static-blue-500`, `primary-600`
 * - CSS 변수: `var(--static-blue-500, #3688FF)`, `var(--color-blue-500)` (대체값은 버린다)
 */
export function resolveTextColor(value: string) {
  const raw = value.trim().toLowerCase();
  const variable = /^var\(\s*--(?:color-)?([a-z0-9-]+)\s*(?:,[^)]*)?\)$/.exec(raw)?.[1];
  const token = NAMED_COLORS[raw] ?? variable ?? raw;
  const match = /^(static-)?(primary|[a-z]+)-(\d+)$/.exec(token);
  if (!match) return undefined;

  const [, staticPrefix, name, step] = match;
  // primary는 blue 토큰의 별칭이다.
  const hue = name === 'primary' ? 'blue' : name!;
  if (!COLOR_STEPS.includes(step!)) return undefined;
  if (staticPrefix) return STATIC_HUES.includes(hue) ? `var(--static-${hue}-${step})` : undefined;
  return ADAPTIVE_HUES.includes(hue) ? `var(--adaptive-${hue}-${step})` : undefined;
}

const IMAGE_SIZE = /^(.*?)\s*\|\s*(\d{1,4})(%|px)?(?:\s*x\s*(\d{1,4})(?:px)?)?\s*$/i;

/**
 * 이미지 대체 텍스트 끝의 `|크기`를 떼어 낸다. 서버가 본문의 `<img>` 태그를 막아서, 크기는
 * 마크다운 문법 `![설명|300](주소)` 안에 적는다.
 *
 * - `설명|300`: 너비 300px
 * - `설명|50%`: 너비 50%
 * - `설명|300x200`: 너비 300px, 높이 200px
 * - `설명|x200`은 지원하지 않는다. 높이만 맞추려면 너비를 함께 적는다.
 */
export function parseImageSize(alt: string) {
  const match = IMAGE_SIZE.exec(alt);
  if (!match) return { alt };

  const [, text, width, unit, height] = match;
  return {
    alt: text!,
    width: unit === '%' ? `${width}%` : `${width}px`,
    height: height && unit !== '%' ? `${height}px` : undefined,
  };
}
