import { parseImageSize, resolveTextColor, toPlainText } from '@/utils/markdown';

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

describe('resolveTextColor', () => {
  it.each([
    ['red', 'var(--adaptive-red-600)'],
    ['Primary', 'var(--adaptive-blue-600)'],
    ['blue-500', 'var(--adaptive-blue-500)'],
    ['static-blue-500', 'var(--static-blue-500)'],
    ['var(--static-blue-500, #3688FF)', 'var(--static-blue-500)'],
    ['var(--color-green-700)', 'var(--adaptive-green-700)'],
  ])('%s는 %s로 칠한다', (value, expected) => {
    expect(resolveTextColor(value)).toBe(expected);
  });

  it.each(['#3688FF', 'rgb(0,0,0)', 'blue-550', 'static-gray-500', 'purple-500', 'var(--x)'])(
    '토큰이 아닌 %s는 무시한다',
    (value) => {
      expect(resolveTextColor(value)).toBeUndefined();
    },
  );
});

describe('parseImageSize', () => {
  it.each([
    ['회고 사진|300', { alt: '회고 사진', width: '300px', height: undefined }],
    ['회고 사진 | 300px', { alt: '회고 사진', width: '300px', height: undefined }],
    ['회고 사진|50%', { alt: '회고 사진', width: '50%', height: undefined }],
    ['회고 사진|300x200', { alt: '회고 사진', width: '300px', height: '200px' }],
  ])('%s의 크기를 떼어 낸다', (alt, expected) => {
    expect(parseImageSize(alt)).toEqual(expected);
  });

  it.each(['회고 사진', 'A|B 비교', '회고|x200'])('크기가 아닌 %s는 그대로 둔다', (alt) => {
    expect(parseImageSize(alt)).toEqual({ alt });
  });
});
