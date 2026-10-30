/// <reference types="node" />

import { existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import express from 'express';
import { pipeline } from 'node:stream/promises';
import { getPreloadFiles, loadBuildAssets } from '../ssg/assets';
import { resolveHtmlPath } from '../ssg/paths';
import { createPathRouter } from '../ssg/routes';
import { renderStreamingPage } from './render';

// 이 파일은 webpack이 .build/server.cjs로 번들해 실행하므로, 경로는 번들 위치(.build/) 기준입니다.
// 배포 환경처럼 폴더 구조가 다르면 DIST_DIR로 지정합니다.
const DIST_DIR = process.env.DIST_DIR ?? fileURLToPath(new URL('../dist/', import.meta.url));
const CSR_SHELL_PATH = path.join(DIST_DIR, 'index.html');
const PORT = Number(process.env.PORT ?? 3000);

if (!existsSync(CSR_SHELL_PATH)) {
  throw new Error(`${CSR_SHELL_PATH}가 없습니다. npm run build로 먼저 빌드하세요.`);
}

// 같은 빌드 dist에서 사용하는 script/CSS/preload 정보
const buildAssets = loadBuildAssets(path.join(DIST_DIR, 'build-assets.json'));

// 빌드로 인해 생성된 HTML 있는 경우 해당 파일 경로를 반환하고, 없는 경우 dist/index.html 사용
function findPrebuiltHtml(pathname: string): string {
  const htmlPath = resolveHtmlPath(DIST_DIR, pathname);
  return htmlPath && existsSync(htmlPath) ? htmlPath : CSR_SHELL_PATH;
}

const router = createPathRouter();

function countPathSegments(pathname: string): number {
  return pathname.split('/').filter(Boolean).length;
}

// 경로 전체가 라우트에 맞으면 매치된 라우트 목록(루트부터)을, 아니면 null을 돌려줍니다.
// 라우터는 /community/123/extra처럼 뒤가 남는 경로도 가장 깊게 맞은 라우트(/community/$feedId)까지
// 매치해 돌려주므로, 그 매치가 요청 경로의 세그먼트를 모두 덮었는지 비교합니다.
// 맞는 라우트가 하나도 없으면 루트 라우트만 남습니다.
function matchPage(pathname: string) {
  const matches = router.matchRoutes(pathname);
  const deepest = matches.at(-1);

  if (matches.length <= 1 || !deepest) return null;
  if (countPathSegments(deepest.pathname) !== countPathSegments(pathname)) return null;
  return matches;
}

// TanStack Router의 요청 핸들러는 웹 표준 Request를 받으므로 express 요청을 옮겨 담습니다.
// 사용자가 응답을 다 받기 전에 연결을 끊으면 signal로 알려 서버 렌더를 멈추게 합니다.
function toWebRequest(req: express.Request, res: express.Response): Request {
  const controller = new AbortController();
  res.on('close', () => {
    if (!res.writableFinished) controller.abort();
  });

  const headers = new Headers();
  for (const [key, value] of Object.entries(req.headers)) {
    if (Array.isArray(value)) value.forEach((item) => headers.append(key, item));
    else if (value !== undefined) headers.set(key, value);
  }

  return new Request(new URL(req.originalUrl, `${req.protocol}://${req.get('host')}`), {
    headers,
    signal: controller.signal,
  });
}

const app = express();

app.get('/health', (_req, res) => {
  res.send('ok');
});

// 로컬 환경 전용 dist server
if (process.env.SERVE_STATIC === 'true') {
  app.use(express.static(DIST_DIR, { index: false, redirect: false }));
}

app.get('/{*path}', async (req, res) => {
  const matches = matchPage(req.path);

  // 라우트 목록에 없는 경로: 오타 주소, 뒤에 세그먼트가 더 붙은 주소, 서버로 잘못 들어온 파일 요청(/main.abc.js)
  // 본문은 CSR 셸을 그대로 줘서 브라우저가 "없는 페이지" 화면을 그리게 하고, 상태 코드만 404로 알립니다.
  if (!matches) {
    res.status(404).sendFile(CSR_SHELL_PATH);
    return;
  }

  if (matches.some((match) => match.staticData?.ssr === true)) {
    let page;
    try {
      page = await renderStreamingPage(toWebRequest(req, res), {
        buildAssets,
        preloadFiles: getPreloadFiles(
          buildAssets,
          matches.map((match) => match.routeId),
        ),
      });
    } catch (error) {
      // 셸을 만들기 전에 실패하면 아직 응답을 시작하지 않았으므로 아래 SSG·CSR 셸로 넘어갑니다.
      console.error(`[ssr] ${req.originalUrl} 렌더 실패`, error);
    }

    if (page) {
      res.status(page.status);
      page.headers.forEach((value, key) => res.setHeader(key, value));
      res.setHeader('Cache-Control', 'no-cache');

      if (!page.body) {
        res.end();
        return;
      }
      // 응답을 시작한 뒤에는 상태 코드를 바꿀 수 없어, 전송 중 끊기면 로그만 남깁니다.
      await pipeline(page.body, res).catch((error: unknown) => {
        console.error(`[ssr] ${req.originalUrl} 전송 중단`, error);
      });
      return;
    }
  }

  res.setHeader('Cache-Control', 'no-cache');
  res.sendFile(findPrebuiltHtml(req.path));
});

app.listen(PORT, (error) => {
  if (error) throw error;
  console.log(`[ssr] http://localhost:${PORT}`);
});
