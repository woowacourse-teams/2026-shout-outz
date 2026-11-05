/// <reference types="node" />
import path from 'node:path';

// pathname에 해당하는 dist/…/index.html 절대 경로를 돌려줍니다. SSG가 HTML을 쓰는 위치와 서버가 HTML을
// 찾는 위치가 같은 규칙을 따르게 합니다. 경로가 dist 밖을 가리키면(예: /../etc) null을 돌려줍니다.
export function resolveHtmlPath(distDir: string, pathname: string): string | null {
  const htmlPath = path.resolve(distDir, ...pathname.split('/').filter(Boolean), 'index.html');
  const relativePath = path.relative(distDir, htmlPath);

  if (relativePath.startsWith('..') || path.isAbsolute(relativePath)) return null;
  return htmlPath;
}
