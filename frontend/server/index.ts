/// <reference types="node" />

import { existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import express from 'express';
import { resolveHtmlPath } from '../ssg/paths';
import { createPathRouter } from '../ssg/routes';

// 이 파일은 webpack이 ssg/.build/server.cjs로 번들해 실행하므로, 경로는 번들 위치(ssg/.build/) 기준입니다.
// 배포 환경처럼 폴더 구조가 다르면 DIST_DIR로 지정합니다.
const DIST_DIR = process.env.DIST_DIR ?? fileURLToPath(new URL('../../dist/', import.meta.url));
const CSR_SHELL_PATH = path.join(DIST_DIR, 'index.html');
const PORT = Number(process.env.PORT ?? 3000);

if (!existsSync(CSR_SHELL_PATH)) {
  throw new Error(`${CSR_SHELL_PATH}가 없습니다. npm run build로 먼저 빌드하세요.`);
}

// 빌드로 인해 생성된 HTML 있는 경우 해당 파일 경로를 반환하고, 없는 경우 dist/index.html 사용
function findPrebuiltHtml(pathname: string): string {
  const htmlPath = resolveHtmlPath(DIST_DIR, pathname);
  return htmlPath && existsSync(htmlPath) ? htmlPath : CSR_SHELL_PATH;
}

const router = createPathRouter();

function countPathSegments(pathname: string): number {
  return pathname.split('/').filter(Boolean).length;
}

// 경로 전체가 어떤 라우트에 맞는지 확인합니다. 라우터는 /community/123/extra처럼 뒤가 남는 경로도
// 가장 깊게 맞은 라우트(/community/$feedId)까지 매치해 돌려주므로, 그 매치가 요청 경로의 세그먼트를
// 모두 덮었는지 비교합니다. 맞는 라우트가 하나도 없으면 루트 라우트만 남습니다.
function isKnownPath(pathname: string): boolean {
  const matches = router.matchRoutes(pathname);
  const deepest = matches.at(-1);

  if (matches.length <= 1 || !deepest) return false;
  return countPathSegments(deepest.pathname) === countPathSegments(pathname);
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
  // 라우트 목록에 없는 경로: 오타 주소, 뒤에 세그먼트가 더 붙은 주소, 서버로 잘못 들어온 파일 요청(/main.abc.js)
  // 본문은 CSR 셸을 그대로 줘서 브라우저가 "없는 페이지" 화면을 그리게 하고, 상태 코드만 404로 알립니다.
  if (!isKnownPath(req.path)) {
    res.status(404).sendFile(CSR_SHELL_PATH);
    return;
  }

  // TODO: staticData.ssr 라우트는 여기서 ssg/render.tsx로 렌더합니다. webpack 설정 후 연결합니다.
  res.setHeader('Cache-Control', 'no-cache');
  res.sendFile(findPrebuiltHtml(req.path));
});

app.listen(PORT, (error) => {
  if (error) throw error;
  console.log(`[ssr] http://localhost:${PORT}`);
});
