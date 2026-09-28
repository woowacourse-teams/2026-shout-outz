import { toPlainText } from '@/utils/markdown';

describe('toPlainText', () => {
  test('제목·목록·인용 기호를 걷어내고 줄을 공백 하나로 합친다', () => {
    expect(toPlainText('## 소개\n\n- 첫째\n- 둘째\n> 인용')).toBe('소개 첫째 둘째 인용');
  });

  test('강조와 인라인 코드는 글자만 남긴다', () => {
    expect(toPlainText('**굵게** _기울임_ ~~취소~~ `code`')).toBe('굵게 기울임 취소 code');
  });

  test('링크는 글자만 남기고 이미지와 코드 블록은 뺀다', () => {
    expect(
      toPlainText(
        '[블로그](https://a.com) 참고\n\n![그림](https://a.com/x.png)\n```ts\nconst a = 1;\n```\n끝',
      ),
    ).toBe('블로그 참고 끝');
  });

  test('닫히지 않은 코드 블록은 끝까지 뺀다', () => {
    expect(toPlainText('앞\n```ts\nconst a = 1;')).toBe('앞');
  });

  test('주소를 그대로 적은 본문은 주소를 남긴다', () => {
    expect(toPlainText('후기입니다.\nhttps://woojin.log/tech')).toBe(
      '후기입니다. https://woojin.log/tech',
    );
  });
});
