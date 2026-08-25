/// <reference types="node" />
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRouter, createMemoryHistory, type AnyRouter } from '@tanstack/react-router';
import { routeTree } from '../routeTree.gen';
import { renderCsrShell, renderPage } from '../entry-ssg';
import { collectPrerenderRoutes } from './prerenderRoutes';
import { injectBuildAssets } from './injectBuildAssets';
import type { BuildAssets } from '../buildAssets';

// url은 buildLocation을 거쳐 이미 정규화/디코딩된 pathname이지만(src/ssg/prerenderRoutes.ts),
// generateStaticParams가 돌려주는 파라미터 값 자체는 우리가 신뢰할 수 없는 데이터일 수 있습니다.
// path.resolve로 만든 최종 경로가 여전히 distDir 안에 있는지 확인해 경로 이탈(예: id에 '..' 포함)을 막습니다.
function urlToOutputPath(distDir: string, url: string): string {
    const distDirAbs = path.resolve(distDir);
    const segments = url.split('/').filter(Boolean);
    const resolved = path.resolve(distDirAbs, ...segments, 'index.html');

    const relative = path.relative(distDirAbs, resolved);
    if (relative.startsWith('..') || path.isAbsolute(relative)) {
        throw new Error(`[ssg] "${url}"이(가) dist 디렉터리 밖의 경로(${resolved})를 만듭니다.`);
    }

    return resolved;
}

async function main() {
    // 이 소스는 src/ssg/에 있지만, 실행되는 건 webpack이 만든 dist-server/generate-static-site.cjs
    // 번들 하나뿐이라 import.meta.url은 그 출력 파일 기준입니다(소스 트리 깊이와 무관).
    const distDir = fileURLToPath(new URL('../dist/', import.meta.url));
    const buildAssetsPath = path.join(distDir, 'build-assets.json');

    if (!existsSync(buildAssetsPath)) {
        throw new Error(
            `빌드 매니페스트를 찾을 수 없습니다: ${buildAssetsPath}\n먼저 'npm run build:dev'(또는 build)를 실행해 dist/build-assets.json을 만들어주세요.`
        );
    }

    // Webpack client build가 WebpackManifestPlugin으로 emit한, 실제(해시 포함) 파일명 계약입니다.
    const assets: BuildAssets = JSON.parse(readFileSync(buildAssetsPath, 'utf-8'));

    // `node generate-static-site.cjs <url>`처럼 특정 URL 하나만 넘기면, 전체 사이트를 돌리는 대신
    // entry-ssg.tsx의 renderPage(url) 결과만 렌더링해 그 한 페이지만 씁니다(디버깅/단일 페이지 재생성용).
    const [, , singleUrl] = process.argv;
    if (singleUrl) {
        const html = injectBuildAssets(await renderPage(singleUrl), assets);
        const outputPath = urlToOutputPath(distDir, singleUrl);
        mkdirSync(path.dirname(outputPath), { recursive: true });
        writeFileSync(outputPath, html);
        console.log(`[ssg] ${singleUrl} -> ${path.relative(distDir, outputPath)} (${html.length} bytes)`);
        return;
    }

    // CSR과 SSG 모두 동일한 injectBuildAssets()로 asset을 심습니다.
    const csrHtml = injectBuildAssets(await renderCsrShell(), assets);
    writeFileSync(path.join(distDir, 'index.html'), csrHtml);
    console.log(`[csr] -> dist/index.html (${csrHtml.length} bytes)`);

    // routeTree만 있으면 매치 상태 없이도 각 라우트의 staticData를 읽을 수 있어,
    // 프리렌더 대상 조회 전용으로 가벼운 라우터 인스턴스를 하나 더 만듭니다.
    const discoveryRouter = createRouter({
        routeTree,
        history: createMemoryHistory({ initialEntries: ['/'] }),
    });
    const routesToPrerender = await collectPrerenderRoutes(discoveryRouter as AnyRouter);

    if (routesToPrerender.length === 0) {
        console.warn('[ssg] staticData.prerender가 true인 라우트를 찾지 못했습니다.');
    }

    for (const url of routesToPrerender) {
        const html = injectBuildAssets(await renderPage(url), assets);

        const outputPath = urlToOutputPath(distDir, url);
        mkdirSync(path.dirname(outputPath), { recursive: true });
        writeFileSync(outputPath, html);
        console.log(`[ssg] ${url} -> ${path.relative(distDir, outputPath)} (${html.length} bytes)`);
    }
}

main().catch((error: unknown) => {
    console.error(error);
    process.exitCode = 1;
});
