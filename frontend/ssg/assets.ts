/// <reference types="node" />
import { existsSync, readFileSync } from 'node:fs';

export type BuildAssets = {
  /** main entry를 이루는 script입니다. splitChunks가 framework 등을 떼어내면 여러 개가 됩니다. */
  scripts: string[];
  styles?: string[];
};

function isBuildAssets(value: unknown): value is BuildAssets {
  if (typeof value !== 'object' || value === null) return false;

  const assets = value as Record<string, unknown>;
  return (
    isFileList(assets.scripts) &&
    assets.scripts.length > 0 &&
    (assets.styles === undefined || isFileList(assets.styles))
  );
}

function isFileList(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((file) => typeof file === 'string' && file.length > 0);
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
        `먼저 'npm run build:dev'(또는 build)를 실행해 dist/build-assets.json을 만들어주세요.`,
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
  const styleLinks = (assets.styles ?? [])
    .map((file) => `<link rel="stylesheet" href="${file}">`)
    .join('');
  if (styleLinks) {
    result = insertBefore(result, '</head>', styleLinks);
  }

  // module script는 받는 대로 병렬로 내려받고 문서 순서대로 실행됩니다. webpack 런타임이 entry를 이루는
  // 청크가 모두 도착했는지 확인한 뒤 main을 시작하므로 순서에 의존하지 않습니다.
  const scriptTags = assets.scripts
    .map((file) => `<script type="module" src="${file}"></script>`)
    .join('');
  return insertBefore(result, '</body>', scriptTags);
}
