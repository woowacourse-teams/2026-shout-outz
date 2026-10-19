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
