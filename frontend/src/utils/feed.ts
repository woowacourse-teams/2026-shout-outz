const FIRST_URL_PATTERN = /https?:\/\/[^\s<>()]+/;

/** 본문에서 첫 번째 주소를 찾는다. 링크 미리보기에 쓰며, 문장 끝 구두점은 떼어낸다. */
export function findFirstUrl(content: string) {
  return content.match(FIRST_URL_PATTERN)?.[0].replace(/[.,!?;:]+$/, '');
}
