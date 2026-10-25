/// <reference types="node" />

import { existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import express from 'express';

const DIST_DIR = fileURLToPath(new URL('../dist/', import.meta.url));
const CSR_SHELL_PATH = path.join(DIST_DIR, 'index.html');
const PORT = Number(process.env.PORT ?? 3000);

if (!existsSync(CSR_SHELL_PATH)) {
  throw new Error(`${CSR_SHELL_PATH}가 없습니다. npm run build로 먼저 빌드하세요.`);
}

// 빌드로 인해 생성된 HTML 있는 경우 해당 파일 경로를 반환하고, 없는 경우 dist/index.html 사용
function findPrebuiltHtml(pathname: string): string {
  const htmlPath = path.resolve(DIST_DIR, ...pathname.split('/').filter(Boolean), 'index.html');
  const relativePath = path.relative(DIST_DIR, htmlPath);

  if (relativePath.startsWith('..') || path.isAbsolute(relativePath) || !existsSync(htmlPath)) {
    return CSR_SHELL_PATH;
  }
  return htmlPath;
}

const app = express();

app.get('/health', (_req, res) => {
  res.send('ok');
});

// 로컬 환경 전용 dist server
if (process.env.SERVE_STATIC === 'true') {
  app.use(express.static(DIST_DIR, { index: false, redirect: false }));
}

app.get('/{*path}', (req, res) => {
  // TODO: staticData.ssr 라우트는 여기서 ssg/render.tsx로 렌더합니다. webpack 설정 후 연결합니다.
  res.setHeader('Cache-Control', 'no-cache');
  res.sendFile(findPrebuiltHtml(req.path));
});

app.listen(PORT, (error) => {
  if (error) throw error;
  console.log(`[ssr] http://localhost:${PORT}`);
});
