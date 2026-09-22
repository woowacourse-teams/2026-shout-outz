/// <reference types="node" />

import { mkdirSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { renderCsrShell, renderStaticPage } from './render';
import { injectBuildAssets, loadBuildAssets, type BuildAssets } from './assets';
import { collectPrerenderRoutes } from './routes';

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

function getOutputPath(distDir: string, url: string): string {
  const outputPath = path.resolve(distDir, ...url.split('/').filter(Boolean), 'index.html');
  const relativePath = path.relative(distDir, outputPath);

  if (relativePath.startsWith('..') || path.isAbsolute(relativePath)) {
    throw new Error(`Prerender output escaped the dist directory: ${url}`);
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
): Promise<PageGenerationSuccess> {
  const url = normalizeUrl(inputUrl);
  const html = injectBuildAssets(await renderStaticPage(url), assets);
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

async function generateCsrShell(distDir: string, assets: BuildAssets): Promise<void> {
  const html = injectBuildAssets(await renderCsrShell(), assets);
  writeHtml(path.join(distDir, 'index.html'), html);
  console.log(`[csr] -> dist/index.html (${html.length} bytes)`);
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
  const distDir = fileURLToPath(new URL('../../dist/', import.meta.url));
  const target = process.argv[2];

  if (target === '--csr-shell') {
    await generateCsrShell(distDir, { script: '/main.js' });
    return;
  }

  const assets = loadBuildAssets(path.join(distDir, 'build-assets.json'));

  if (target) {
    const results: PageGenerationResult[] = [];
    try {
      results.push(await generatePage(distDir, target, assets));
    } catch (error) {
      results.push({ status: 'failed', url: target, stage: 'page render', error });
    }
    printGenerationSummary(results);
    return;
  }

  await generateCsrShell(distDir, assets);

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
      results.push(await generatePage(distDir, url, assets));
    } catch (error) {
      results.push({ status: 'failed', url, stage: 'page render', error });
    }
  }

  printGenerationSummary(results);
}

void main().catch((error: unknown) => {
  console.error(error);
  process.exitCode = 1;
});
