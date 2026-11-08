import { Readable } from 'node:stream';
import { withBuildScripts } from './BuildAssetTags';

const SCRIPT = '<script type="module" src="/main.js"></script>';

async function run(chunks: string[]): Promise<string> {
  const source = Readable.from(chunks.map((chunk) => Buffer.from(chunk)));
  let html = '';
  for await (const chunk of withBuildScripts(source, ['/main.js'])) html += chunk;
  return html;
}

describe('withBuildScripts', () => {
  it('main script를 </body> 바로 앞에 넣는다', async () => {
    expect(await run(['<html><body><div></div></body></html>'])).toBe(
      `<html><body><div></div>${SCRIPT}</body></html>`,
    );
  });

  it('</body>가 두 조각에 걸쳐 와도 찾는다', async () => {
    expect(await run(['<html><body><div></div></bo', 'dy></html>'])).toBe(
      `<html><body><div></div>${SCRIPT}</body></html>`,
    );
  });

  it('조각을 나누는 위치에 이모지가 걸려도 깨지지 않는다', async () => {
    // 첫 조각의 끝 6칸을 다음 조각과 이어 보려고 남기는데, 그 경계가 🧸(UTF-16 두 칸) 가운데에 놓이는 경우
    expect(await run(['ab🧸cdefg', '</body></html>'])).toBe(`ab🧸cdefg${SCRIPT}</body></html>`);
  });
});
