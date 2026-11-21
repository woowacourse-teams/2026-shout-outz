import { Transform } from 'node:stream';
import { StringDecoder } from 'node:string_decoder';

const BODY_END = '</body>';

// main script를 </body> 직전에 끼워 넣습니다. React의 bootstrapModules는 async를 붙여, 스트림 끝에 오는
// $_TSR보다 main이 먼저 실행될 수 있습니다. async가 없는 module script는 문서를 다 읽은 뒤 실행됩니다.
// </body>가 두 조각에 걸쳐 올 수 있어 마지막 몇 글자는 다음 조각과 이어 붙여 찾습니다.
// 넣을 HTML은 </body>를 만난 순간에 만듭니다. 그때는 Suspense가 모두 풀려 렌더 중 받은 쿼리가 캐시에 있습니다.
export function insertBeforeBodyEnd(html: () => string): Transform {
  const decoder = new StringDecoder('utf-8');
  let pending = '';
  let inserted = false;

  return new Transform({
    transform(chunk: Buffer, _encoding, callback) {
      const text = pending + decoder.write(chunk);
      if (inserted) return callback(null, text);

      const index = text.indexOf(BODY_END);
      if (index !== -1) {
        inserted = true;
        pending = '';
        return callback(null, text.slice(0, index) + html() + text.slice(index));
      }

      const keep = BODY_END.length - 1;
      pending = text.slice(-keep);
      callback(null, text.slice(0, -keep));
    },
    flush(callback) {
      callback(null, pending + decoder.end());
    },
  });
}
