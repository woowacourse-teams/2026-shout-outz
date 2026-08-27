/// <reference types="node" />
import { existsSync, readFileSync } from 'node:fs';

export type BuildAssets = {
    script: string;
    style?: string;
};

function isBuildAssets(value: unknown): value is BuildAssets {
    if (typeof value !== 'object' || value === null) return false;

    const assets = value as Record<string, unknown>;
    return (
        typeof assets.script === 'string' &&
        assets.script.length > 0 &&
        (assets.style === undefined || (typeof assets.style === 'string' && assets.style.length > 0))
    );
}

function insertBefore(html: string, marker: string, insertion: string): string {
    const index = html.indexOf(marker);

    if (index === -1) {
        throw new Error(`injectBuildAssets: "${marker}"를 HTML에서 찾지 못했습니다.`);
    }

    return `${html.slice(0, index)}${insertion}${html.slice(index)}`;
}

export function loadBuildAssets(manifestPath: string): BuildAssets {
    if (!existsSync(manifestPath)) {
        throw new Error(
            `빌드 매니페스트를 찾을 수 없습니다: ${manifestPath}\n` +
                `먼저 'npm run build:dev'(또는 build)를 실행해 dist/build-assets.json을 만들어주세요.`
        );
    }

    const assets: unknown = JSON.parse(readFileSync(manifestPath, 'utf-8'));
    if (!isBuildAssets(assets)) {
        throw new Error(`빌드 매니페스트 형식이 올바르지 않습니다: ${manifestPath}`);
    }

    return assets;
}

export function injectBuildAssets(html: string, assets: BuildAssets): string {
    let result = html;

    // CSS는 렌더 차단 리소스라 <head>에 두어 FOUC를 방지합니다.
    if (assets.style) {
        result = insertBefore(result, '</head>', `<link rel="stylesheet" href="${assets.style}">`);
    }

    return insertBefore(result, '</body>', `<script type="module" src="${assets.script}"></script>`);
}
