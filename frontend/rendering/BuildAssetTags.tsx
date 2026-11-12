/// <reference types="node" />
import { Transform, type Readable } from 'node:stream';
import { StringDecoder } from 'node:string_decoder';
import { preinit, preload } from 'react-dom';
import type { BuildAssets, PreloadFiles } from './buildAssets';

/** 한 페이지 HTML에 넣을 빌드 자산입니다. */
export type PageAssets = {
  buildAssets: BuildAssets;
  preloadFiles: PreloadFiles;
};

// 컴포넌트 안에서 만드는 메타 정보는 메모리 렌더 -> 전송으로 <head> 호이스팅이 되지만
// 스트리밍 SSR에서는 React가 빌드 파일 이름을 알 수 없기 때문에(build-assets.json에 있는) 직접 읽어서 preinit, preload 처리
export function BuildAssetLinks({ buildAssets, preloadFiles }: PageAssets) {
  for (const file of buildAssets.styles ?? []) {
    preinit(file, { as: 'style' });
  }
  for (const file of preloadFiles.critical ?? []) {
    preload(file, { as: 'script' });
  }
  for (const file of preloadFiles.lowPriority ?? []) {
    preload(file, { as: 'script', fetchPriority: 'low' });
  }
  return null;
}

const BODY_END = '</body>';

function isHighSurrogate(code: number): boolean {
  return code >= 0xd800 && code <= 0xdbff;
}

// main script를 </body> 직전에 끼워 넣습니다. React의 bootstrapModules는 async를 붙여, 그보다 앞에 오는
// $_TSR보다 main이 먼저 실행될 수 있습니다. async가 없는 module script는 문서를 다 읽은 뒤 실행됩니다.
// </body>가 두 조각에 걸쳐 올 수 있어 마지막 몇 글자는 다음 조각과 이어 붙여 찾습니다.
function insertBeforeBodyEnd(html: string): Transform {
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
        return callback(null, text.slice(0, index) + html + text.slice(index));
      }

      // 이모지처럼 UTF-16 두 칸(서로게이트 쌍)인 글자를 가운데서 자르면 깨진 글자로 나가므로, 자를 위치가
      // 쌍의 앞 칸 바로 뒤라면 한 칸 앞에서 자릅니다.
      let cut = Math.max(0, text.length - (BODY_END.length - 1));
      if (cut > 0 && isHighSurrogate(text.charCodeAt(cut - 1))) cut -= 1;
      pending = text.slice(cut);
      callback(null, text.slice(0, cut));
    },
    flush(callback) {
      callback(null, pending + decoder.end());
    },
  });
}

export function withBuildScripts(source: Readable, scripts: string[]): Readable {
  const output = insertBeforeBodyEnd(
    scripts.map((file) => `<script type="module" src="${file}"></script>`).join(''),
  );
  source.on('error', (error) => output.destroy(error));
  return source.pipe(output);
}
