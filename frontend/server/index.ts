/// <reference types="node" />

import { existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import express from 'express';
import { isHTTPError } from 'ky';
import { pipeline } from 'node:stream/promises';
import { getPreloadFiles, loadBuildAssets } from '../rendering/buildAssets';
import { resolveHtmlPath } from '../rendering/htmlPath';
import { matchPage } from './matchPage';
import { renderStreamingPage } from './render';
import { toWebRequest } from './webRequest';

// 로컬과 배포 파일에서 기준 경로가 다른 것을 맞춰주기 위함
const DIST_DIR = process.env.DIST_DIR ?? fileURLToPath(new URL('../dist/', import.meta.url));
const CSR_SHELL_PATH = path.join(DIST_DIR, 'index.html');
const PORT = Number(process.env.PORT ?? 3000);

if (!existsSync(CSR_SHELL_PATH)) {
  throw new Error(`${CSR_SHELL_PATH}가 없습니다. npm run build로 먼저 빌드하세요.`);
}

// 같은 빌드 dist에서 사용하는 script/CSS/preload 정보
const buildAssets = loadBuildAssets(path.join(DIST_DIR, 'build-assets.json'));

// 서버에서는 렌더가 실패했을 때 사용
function findPrebuiltHtml(pathname: string): string {
  const htmlPath = resolveHtmlPath(DIST_DIR, pathname);
  return htmlPath && existsSync(htmlPath) ? htmlPath : CSR_SHELL_PATH;
}

const app = express();

app.set('trust proxy', 1);

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
        queries: matches.flatMap(
          (match) => match.staticData?.ssrQueries?.(match.params as Record<string, string>) ?? [],
        ),
      });
    } catch (error) {
      // 서버 렌더 전에 받은 데이터가 없다면(삭제된 글 등) 위의 없는 경로와 같이 CSR 셸 + 404로 응답합니다.
      if (isHTTPError(error) && error.response.status === 404) {
        res.status(404).sendFile(CSR_SHELL_PATH);
        return;
      }
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
