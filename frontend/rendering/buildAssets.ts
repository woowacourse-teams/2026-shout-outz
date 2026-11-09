/// <reference types="node" />
import { existsSync, readFileSync } from 'node:fs';

export type BuildAssets = {
  scripts: string[]; // 진입점에서 import문을 따라가 수집 된 청크들(framework, lib, bundle, ...)
  styles?: string[];
  routeChunks?: Record<string, string[]>; // "/community/$feedId": ["1.chunk.js", "2.chunk.js", "3.chunk.js", ...]
  importChunks?: Record<string, string[]>; // 동적 import 경로별 만들어진 청크 ex) "posthog-js": ["/posthog.chunk.js"]
};

export type PreloadFiles = {
  critical?: string[]; // 첫 화면에 필요한 청크
  lowPriority?: string[]; // 첫 화면에 필요하지 않은 나머지 청크들
};

function isBuildAssets(value: unknown): value is BuildAssets {
  if (typeof value !== 'object' || value === null) return false;

  const assets = value as Record<string, unknown>;
  return (
    isFileList(assets.scripts) &&
    assets.scripts.length > 0 &&
    (assets.styles === undefined || isFileList(assets.styles)) &&
    (assets.routeChunks === undefined || isChunkFilesRecord(assets.routeChunks)) &&
    (assets.importChunks === undefined || isChunkFilesRecord(assets.importChunks))
  );
}

function isFileList(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((file) => typeof file === 'string' && file.length > 0);
}

function isChunkFilesRecord(value: unknown): value is Record<string, string[]> {
  if (typeof value !== 'object' || value === null) return false;

  return Object.values(value).every(isFileList);
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
function getRouteChunkFiles(assets: BuildAssets, routeIds: string[]): string[] {
  return [...new Set(routeIds.flatMap((routeId) => assets.routeChunks?.[routeId] ?? []))];
}

// 페이지 HTML의 <head>에 미리 받아 둘(preload) 청크 목록을 정합니다.
// - critical: 이 페이지의 라우트 청크입니다. preload가 없으면 main.js가 실행된 뒤에야 요청되어 늦어집니다.
// - lowPriority: PostHog SDK 청크입니다. main 실행 직후 import('posthog-js')로 요청되지만 첫 화면에는 필요 없어
//   낮은 우선순위로 받습니다. SDK는 빌드에 POSTHOG_KEY가 있을 때만 불러오므로(src/utils/analytics/index.ts)
//   같은 조건일 때만 넣습니다.
export function getPreloadFiles(assets: BuildAssets, routeIds: string[]): PreloadFiles {
  return {
    critical: getRouteChunkFiles(assets, routeIds),
    lowPriority: process.env.POSTHOG_KEY ? (assets.importChunks?.['posthog-js'] ?? []) : [],
  };
}
