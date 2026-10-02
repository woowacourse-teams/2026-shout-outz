/**
 * 평문 속 `http(s)://` 주소만 새 탭 링크로 바꾼다.
 *
 * 마크다운으로 해석하지 않아서 `#`, `*` 같은 기호는 쓴 그대로 보인다.
 * 문장 끝에 붙은 `.`, `)` 같은 문장부호는 주소에서 뺀다: `https://a.test).` → `https://a.test`
 */
const URL_PATTERN = /(https?:\/\/[^\s<]*[^\s<.,:;!?'")\]])/g;

export function LinkifiedText({ text }: { text: string }) {
  return text.split(URL_PATTERN).map((part, index) =>
    // split에 캡처 그룹을 넘기면 홀수 번째 조각이 매칭된 주소다.
    index % 2 === 1 ? (
      <a
        key={index}
        href={part}
        target="_blank"
        rel="noopener noreferrer"
        className="text-primary-600 underline"
      >
        {part}
      </a>
    ) : (
      part
    ),
  );
}
