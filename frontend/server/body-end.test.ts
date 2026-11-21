import { Readable } from 'node:stream';
import { insertBeforeBodyEnd } from './body-end';

async function pipeChunks(chunks: string[], html: () => string) {
  let out = '';
  for await (const piece of Readable.from(chunks.map((c) => Buffer.from(c))).pipe(
    insertBeforeBodyEnd(html),
  )) {
    out += piece;
  }
  return out;
}

describe('insertBeforeBodyEnd', () => {
  it('</body> 바로 앞에 넣는다', async () => {
    await expect(
      pipeChunks(['<html><body><main></main></body></html>'], () => '<x/>'),
    ).resolves.toBe('<html><body><main></main><x/></body></html>');
  });

  it('</body>가 두 조각에 걸쳐 와도 찾는다', async () => {
    await expect(pipeChunks(['<body><main></main></bo', 'dy></html>'], () => '<x/>')).resolves.toBe(
      '<body><main></main><x/></body></html>',
    );
  });

  it('넣을 HTML은 </body>를 만난 순간에 만든다(그 전에 도착한 Suspense 결과의 데이터까지 담기도록)', async () => {
    const received: string[] = [];
    const html = jest.fn(() => `<script>${received.join(',')}</script>`);
    const stream = insertBeforeBodyEnd(html);
    let out = '';
    stream.on('data', (piece: Buffer | string) => (out += piece.toString()));
    const done = new Promise((resolve) => stream.on('end', resolve));

    stream.write('<body><!--$?--><template id="B:0"></template><!--/$-->');
    expect(html).not.toHaveBeenCalled();
    // 셸을 보낸 뒤 서버 렌더 중에 데이터를 받고, Suspense 결과가 이어서 온다.
    received.push('list');
    stream.write('<div hidden id="S:0">목록</div></body></html>');
    stream.end();
    await done;

    expect(html).toHaveBeenCalledTimes(1);
    expect(out).toContain('<script>list</script></body>');
  });
});
