/// <reference types="node" />
import path from 'node:path';

// pathname -> 디스크상 경로 반환(해당하는 경로 없으면 null 반환)
export function resolveHtmlPath(distDir: string, pathname: string): string | null {
  const htmlPath = path.resolve(distDir, ...pathname.split('/').filter(Boolean), 'index.html');
  const relativePath = path.relative(distDir, htmlPath);

  if (relativePath.startsWith('..') || path.isAbsolute(relativePath)) return null;
  return htmlPath;
}
