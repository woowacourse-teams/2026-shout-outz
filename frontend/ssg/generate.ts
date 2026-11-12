/// <reference types="node" />

import { mkdirSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { renderCsrShell, renderStaticPage } from './render';
import { getPreloadFiles, injectBuildAssets, loadBuildAssets, type BuildAssets } from './assets';
import {
  collectCsrShellRoutes,
  collectPrerenderRoutes,
  collectSitemapStaticRoutes,
  createRouteIdMatcher,
} from './routes';
import { writeSiteFiles } from './site-files';
import { resolveHtmlPath } from './paths';

const PRERENDER_ORIGIN = 'http://prerender.local';

interface PageGenerationSuccess {
  status: 'success';
  url: string;
  outputPath: string;
  bytes: number;
}

interface PageGenerationFailure {
  status: 'failed';
  url: string;
  stage: 'route collection' | 'page render';
  error: unknown;
}

type PageGenerationResult = PageGenerationSuccess | PageGenerationFailure;

type RouteIdMatcher = (pathname: string) => string[];

function normalizeUrl(input: string): string {
  if (!input.startsWith('/')) {
    throw new Error(`Prerender URL must start with "/": ${input}`);
  }

  const parsed = new URL(input, PRERENDER_ORIGIN);
  if (parsed.origin !== PRERENDER_ORIGIN || parsed.search || parsed.hash) {
    throw new Error(`Prerender URL must be a local pathname: ${input}`);
  }

  return parsed.pathname === '/' ? '/' : parsed.pathname.replace(/\/+$/, '');
}

function getOutputPath(distDir: string, pathname: string): string {
  const outputPath = resolveHtmlPath(distDir, pathname);

  if (!outputPath) {
    throw new Error(`Prerender output escaped the dist directory: ${pathname}`);
  }

  return outputPath;
}

function writeHtml(outputPath: string, html: string): void {
  mkdirSync(path.dirname(outputPath), { recursive: true });
  writeFileSync(outputPath, html);
}

async function generatePage(
  distDir: string,
  inputUrl: string,
  assets: BuildAssets,
  matchRouteIds: RouteIdMatcher,
): Promise<PageGenerationSuccess> {
  const url = normalizeUrl(inputUrl);
  const html = injectBuildAssets(
    await renderStaticPage(url),
    assets,
    getPreloadFiles(assets, matchRouteIds ? matchRouteIds(url) : []),
  );
  const outputPath = getOutputPath(distDir, url);

  writeHtml(outputPath, html);
  console.log(`[ssg] ${url} -> ${path.relative(distDir, outputPath)} (${html.length} bytes)`);
  return {
    status: 'success',
    url,
    outputPath: path.relative(distDir, outputPath),
    bytes: html.length,
  };
}

// '/'의 셸은 dist/index.html이라, 호스팅이 다른 경로의 fallback으로도 이 파일을 내려줍니다.
async function generateCsrShell(
  distDir: string,
  url: string,
  assets: BuildAssets,
  matchRouteIds?: RouteIdMatcher,
): Promise<void> {
  const html = injectBuildAssets(
    await renderCsrShell(),
    assets,
    getPreloadFiles(assets, matchRouteIds ? matchRouteIds(url) : []),
  );
  const outputPath = getOutputPath(distDir, url);

  writeHtml(outputPath, html);
  console.log(`[csr] ${url} -> ${path.relative(distDir, outputPath)} (${html.length} bytes)`);
}

function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

function printGenerationSummary(results: PageGenerationResult[]): void {
  const succeeded = results.filter((result) => result.status === 'success');
  const failed = results.filter((result) => result.status === 'failed');

  console.log('\n[ssg] generation summary');
  for (const result of succeeded) {
    console.log(`  ✓ ${result.url} -> ${result.outputPath} (${result.bytes} bytes)`);
  }
  for (const result of failed) {
    console.error(`  ✗ ${result.url} [${result.stage}] ${getErrorMessage(result.error)}`);
  }
  console.log(`[ssg] ${succeeded.length} succeeded, ${failed.length} failed`);
}

async function main(): Promise<void> {
  const distDir = fileURLToPath(new URL('../dist/', import.meta.url));
  const target = process.argv[2];

  if (target === '--csr-shell') {
    await generateCsrShell(distDir, '/', { scripts: ['/main.js'] });
    return;
  }

  const assets = loadBuildAssets(path.join(distDir, 'build-assets.json'));
  const matchRouteIds = createRouteIdMatcher();

  if (target) {
    const results: PageGenerationResult[] = [];
    try {
      results.push(await generatePage(distDir, target, assets, matchRouteIds));
    } catch (error) {
      results.push({ status: 'failed', url: target, stage: 'page render', error });
    }
    printGenerationSummary(results);
    return;
  }

  for (const url of collectCsrShellRoutes()) {
    await generateCsrShell(distDir, url, assets, matchRouteIds);
  }

  const { routes, failures: routeFailures } = await collectPrerenderRoutes();
  if (routes.length === 0) {
    console.warn('[ssg] staticData.prerender가 true인 라우트를 찾지 못했습니다.');
  }

  const results: PageGenerationResult[] = routeFailures.map(({ route, error }) => ({
    status: 'failed',
    url: route,
    stage: 'route collection',
    error,
  }));

  for (const url of routes) {
    try {
      results.push(await generatePage(distDir, url, assets, matchRouteIds));
    } catch (error) {
      results.push({ status: 'failed', url, stage: 'page render', error });
    }
  }

  printGenerationSummary(results);

  // 실제로 만든 SSG 페이지와, 검색에 노출하기로 한 고정 경로(staticData.sitemap)로 sitemap.xml을 만듭니다.
  const generatedPages = results.flatMap((result) =>
    result.status === 'success' ? [result.url] : [],
  );
  writeSiteFiles(distDir, [...collectSitemapStaticRoutes(), ...generatedPages]);
}

void main().catch((error: unknown) => {
  console.error(error);
  process.exitCode = 1;
});
