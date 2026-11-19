/// <reference types="node" />
import { existsSync, readFileSync } from 'node:fs';

export type BuildAssets = {
  /** main entry를 이루는 script입니다. splitChunks가 framework 등을 떼어내면 여러 개가 됩니다. */
  scripts: string[];
  styles?: string[];
  /** 라우트 id별로 그 라우트 컴포넌트를 그리는 데 필요한 청크 파일 경로입니다. */
  routeChunks?: Record<string, string[]>;
};

export type PreloadFiles = {
  /** 첫 화면을 그리는 데 필요한 청크입니다. */
  critical?: string[];
};

function isBuildAssets(value: unknown): value is BuildAssets {
  if (typeof value !== 'object' || value === null) return false;

  const assets = value as Record<string, unknown>;
  return (
    isFileList(assets.scripts) &&
    assets.scripts.length > 0 &&
    (assets.styles === undefined || isFileList(assets.styles)) &&
    (assets.routeChunks === undefined || isChunkFilesRecord(assets.routeChunks))
  );
}

function isFileList(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((file) => typeof file === 'string' && file.length > 0);
}

function isChunkFilesRecord(value: unknown): value is Record<string, string[]> {
  if (typeof value !== 'object' || value === null) return false;

  return Object.values(value).every(isFileList);
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

// 매치된 라우트들이 쓰는 청크를 중복 없이 모읍니다. 매니페스트에 없는 라우트(분리된 청크가 없는 루트 등)는 건너뜁니다.
export function getRouteChunkFiles(assets: BuildAssets, routeIds: string[]): string[] {
  return [...new Set(routeIds.flatMap((routeId) => assets.routeChunks?.[routeId] ?? []))];
}

// 페이지 HTML의 <head>에 미리 받아 둘(preload) 청크 목록을 정합니다.
// - critical: 이 페이지의 라우트 청크입니다. preload가 없으면 main.js가 실행된 뒤에야 요청되어 늦어집니다.
// PostHog SDK처럼 첫 화면에 필요 없는 청크는 넣지 않습니다. fetchpriority="low"로 preload해도 main과 같은 시간대에
// 내려받아 대역폭을 나눠 쓰므로 main 도착이 늦어지고 FCP·LCP가 늦어집니다. main 실행 뒤 import()가 요청하게 둡니다.
export function getPreloadFiles(assets: BuildAssets, routeIds: string[]): PreloadFiles {
  return { critical: getRouteChunkFiles(assets, routeIds) };
}

function toPreloadLinks(files: string[]): string {
  return files.map((file) => `<link rel="preload" as="script" href="${file}">`).join('');
}

export function injectBuildAssets(
  html: string,
  assets: BuildAssets,
  { critical = [] }: PreloadFiles = {},
): string {
  let result = html;

  // 라우트 청크는 main이 실행된 뒤에야 webpack 런타임이 <script>로 요청합니다. <head>에서 미리 받아 두면
  // main과 병렬로 내려받고, 런타임의 요청은 브라우저 캐시에서 바로 끝납니다.
  // webpack은 청크 <script>에 crossorigin을 붙이지 않으므로 preload에도 붙이지 않아야 캐시가 재사용됩니다.
  const preloadLinks = toPreloadLinks(critical);
  if (preloadLinks) {
    result = insertBefore(result, '</head>', preloadLinks);
  }

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
