import { readFileSync } from 'node:fs';

const PLUGIN_NAME = 'BuildAssetsPlugin';
const ROUTE_ID_PATTERN = /createFileRoute\(\s*['"]([^'"]+)['"]\s*\)/;
// TanStack Router의 autoCodeSplitting은 라우트 파일을 `?tsr-split=component` 같은 쿼리로 다시 import해
// 별도 청크를 만듭니다. 이 쿼리가 붙은 import만 라우트 청크로 봅니다.
const ROUTE_SPLIT_QUERY = 'tsr-split=';

function readRouteId(routeFilePath) {
  const routeId = readFileSync(routeFilePath, 'utf-8').match(ROUTE_ID_PATTERN)?.[1];
  if (!routeId) {
    throw new Error(
      `${PLUGIN_NAME}: ${routeFilePath}에서 createFileRoute의 라우트 id를 찾지 못했습니다.`,
    );
  }
  return routeId;
}

// dist/build-assets.json에 main entry의 scripts/styles와, 라우트 id별로 그 라우트를 그리는 데 필요한
// 청크 파일 목록(routeChunks)을 남깁니다. ssg/generate.ts는 이 목록으로 페이지별 HTML에 preload를 넣어
// main과 라우트 청크를 병렬로 내려받게 합니다.
// importRequests에 적은 동적 import의 청크는 라우트와 무관하게 importChunks에 import 경로별로 남깁니다.
export class BuildAssetsPlugin {
  constructor({ importRequests = [] } = {}) {
    this.importRequests = importRequests;
  }

  apply(compiler) {
    const { Compilation, sources } = compiler.webpack;

    compiler.hooks.thisCompilation.tap(PLUGIN_NAME, (compilation) => {
      compilation.hooks.processAssets.tap(
        { name: PLUGIN_NAME, stage: Compilation.PROCESS_ASSETS_STAGE_REPORT },
        () => {
          // splitChunks가 entry에서 framework 등을 떼어내면 main entry는 여러 파일로 이뤄집니다.
          // HTML은 이 파일을 모두 불러와야 main이 실행됩니다.
          const entryFiles = new Set(compilation.entrypoints.get('main')?.getFiles() ?? []);
          const scripts = [...entryFiles].filter((file) => file.endsWith('.js'));
          const styles = [...entryFiles].filter((file) => file.endsWith('.css'));

          if (scripts.length === 0) {
            throw new Error(`${PLUGIN_NAME}: main entry script를 찾지 못했습니다.`);
          }

          const routeChunks = {};
          for (const chunkGroup of compilation.chunkGroups) {
            for (const origin of chunkGroup.origins) {
              const routeFilePath = origin.module?.resource;
              if (!routeFilePath || !origin.request?.includes(ROUTE_SPLIT_QUERY)) continue;

              // 청크 그룹에는 splitChunks가 떼어낸 shared/lib 청크도 포함됩니다. main entry에 이미 들어간
              // 파일은 HTML이 script로 직접 불러오므로 제외합니다.
              const files = chunkGroup
                .getFiles()
                .filter((file) => file.endsWith('.js') && !entryFiles.has(file))
                .map((file) => `/${file}`);
              const routeId = readRouteId(routeFilePath);
              routeChunks[routeId] = [...new Set([...(routeChunks[routeId] ?? []), ...files])];
            }
          }

          // 동적 import 경로(예: 'posthog-js')로 만들어진 청크 그룹을 찾습니다. swc-loader가 주석을 지워
          // webpackChunkName 매직 주석은 쓸 수 없어 import 경로를 기준으로 삼습니다.
          const importChunks = {};
          for (const request of this.importRequests) {
            const chunkGroup = compilation.chunkGroups.find((group) =>
              group.origins.some((origin) => origin.request === request),
            );
            if (!chunkGroup) {
              throw new Error(
                `${PLUGIN_NAME}: import('${request}')로 만든 청크를 찾지 못했습니다.`,
              );
            }
            importChunks[request] = chunkGroup
              .getFiles()
              .filter((file) => file.endsWith('.js') && !entryFiles.has(file))
              .map((file) => `/${file}`);
          }

          const manifest = {
            scripts: scripts.map((file) => `/${file}`),
            styles: styles.map((file) => `/${file}`),
            routeChunks,
            importChunks,
          };
          compilation.emitAsset(
            'build-assets.json',
            new sources.RawSource(JSON.stringify(manifest, null, 2)),
          );
        },
      );
    });
  }
}
