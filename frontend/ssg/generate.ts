/// <reference types="node" />

import { mkdirSync, rmSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { injectBuildAssets, loadBuildAssets, type BuildAssets } from './assets';
import { renderCsrShell, renderStaticPage } from './render';
import { collectPrerenderRoutes } from './routes';

const PRERENDER_ORIGIN = 'http://prerender.local';

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
    const outputPath = path.resolve(
        distDir,
        ...url.split('/').filter(Boolean),
        'index.html',
    );
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

async function generatePage(distDir: string, inputUrl: string, assets: BuildAssets): Promise<void> {
    const url = normalizeUrl(inputUrl);
    const html = injectBuildAssets(await renderStaticPage(url), assets);
    const outputPath = getOutputPath(distDir, url);

    writeHtml(outputPath, html);
    console.log(`[ssg] ${url} -> ${path.relative(distDir, outputPath)} (${html.length} bytes)`);
}

async function generateCsrShell(distDir: string, assets: BuildAssets): Promise<void> {
    const html = injectBuildAssets(await renderCsrShell(), assets);
    writeHtml(path.join(distDir, 'index.html'), html);
    console.log(`[csr] -> dist/index.html (${html.length} bytes)`);
}

async function main(): Promise<void> {
    const distDir = fileURLToPath(new URL('../../dist/', import.meta.url));
    const target = process.argv[2];

    if (target === '--csr-shell') {
        rmSync(distDir, { recursive: true, force: true });
        await generateCsrShell(distDir, { script: '/main.js' });
        return;
    }

    const assets = loadBuildAssets(path.join(distDir, 'build-assets.json'));

    if (target) {
        await generatePage(distDir, target, assets);
        return;
    }

    await generateCsrShell(distDir, assets);

    const routes = await collectPrerenderRoutes();
    if (routes.length === 0) {
        console.warn('[ssg] staticData.prerender가 true인 라우트를 찾지 못했습니다.');
    }

    for (const url of routes) {
        await generatePage(distDir, url, assets);
    }
}

void main().catch((error: unknown) => {
    console.error(error);
    process.exitCode = 1;
});
