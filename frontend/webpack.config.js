import path from 'node:path';
import { readdirSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import 'webpack-dev-server';
import CopyPlugin from 'copy-webpack-plugin';
import ForkTsCheckerWebpackPlugin from 'fork-ts-checker-webpack-plugin';
import MiniCssExtractPlugin from 'mini-css-extract-plugin';
import { tanstackRouter } from '@tanstack/router-plugin/webpack';
import { BuildAssetsPlugin } from './ssg/build-assets-plugin.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const isProduction = process.env.NODE_ENV === 'production';

function readGitCommit() {
  try {
    return execFileSync('git', ['rev-parse', 'HEAD'], {
      cwd: __dirname,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'ignore'],
    }).trim();
  } catch {
    return undefined;
  }
}

process.env.SENTRY_ENVIRONMENT ||=
  process.env.GITHUB_ACTIONS === 'true'
    ? process.env.GITHUB_REF_NAME === 'main'
      ? 'production'
      : 'development'
    : 'local';
const sentryRelease = process.env.SENTRY_RELEASE || process.env.GITHUB_SHA || readGitCommit();
if (sentryRelease) process.env.SENTRY_RELEASE = sentryRelease;

// 라우트 파일 하나가 페이지 청크 하나가 됩니다(autoCodeSplitting). commons 그룹이 "모든 페이지가 쓰는
// 모듈"을 판단할 때 기준이 되는 페이지 수입니다. __root는 분리되지 않고 main에 들어가므로 세지 않습니다.
function countRouteFiles(dir) {
  return readdirSync(dir, { withFileTypes: true }).reduce((count, entry) => {
    if (entry.isDirectory()) return count + countRouteFiles(path.join(dir, entry.name));
    const isRouteFile = /\.tsx$/.test(entry.name) && !/\.test\.tsx$/.test(entry.name);
    return count + (isRouteFile && entry.name !== '__root.tsx' ? 1 : 0);
  }, 0);
}

const NODE_MODULES_PATTERN = /[\\/]node_modules[\\/]/;
const FRAMEWORK_PATTERN = /[\\/]node_modules[\\/](react|react-dom|scheduler)[\\/]/;
const LIB_MIN_SIZE = 160_000;

function getPackageName(modulePath) {
  return modulePath
    .match(/[\\/]node_modules[\\/]((?:@[^\\/]+[\\/])?[^\\/]+)/)?.[1]
    .replace(/[\\/]/, '-');
}

// 청크 분리 기준(프로덕션 전용)
// - framework: React 등 모든 페이지가 쓰고 거의 바뀌지 않는 코드. 앱 코드가 바뀌어도 캐시가 유지됩니다.
// - lib: 160KB가 넘는 외부 라이브러리 모듈. 패키지별로 따로 캐싱합니다.
// - commons: 모든 페이지 청크가 쓰는 모듈.
// - shared: 2개 이상 페이지 청크가 쓰는 모듈. 쓰는 페이지 조합마다 청크가 따로 생겨 필요한 페이지끼리만 받습니다.
// - page: 그 외 페이지 전용 코드는 라우트 청크(autoCodeSplitting)에 남습니다.
// dev는 csr 셸이 main.js 하나만 불러오므로(ssg/generate.ts --csr-shell) entry를 쪼개지 않습니다.
const splitChunks = {
  chunks: 'all',
  maxInitialRequests: 25,
  minSize: 20_000,
  cacheGroups: {
    default: false,
    defaultVendors: false,
    framework: {
      name: 'framework',
      test: FRAMEWORK_PATTERN,
      priority: 40,
      enforce: true,
    },
    lib: {
      test: (module) =>
        module.size() > LIB_MIN_SIZE && NODE_MODULES_PATTERN.test(module.nameForCondition() ?? ''),
      name: (module) => `lib-${getPackageName(module.nameForCondition() ?? '') ?? 'unknown'}`,
      priority: 30,
      minChunks: 1,
      reuseExistingChunk: true,
    },
    commons: {
      name: 'commons',
      minChunks: countRouteFiles(path.resolve(__dirname, 'src/routes')),
      priority: 20,
    },
    shared: {
      priority: 10,
      minChunks: 2,
      reuseExistingChunk: true,
    },
  },
};

/** @type {import("webpack").Configuration} */
const config = {
  dotenv: {
    dir: __dirname,
    template: ['src/.env', '.env'],
    prefix: [
      'API_ORIGIN',
      'POSTHOG_',
      'SENTRY_DSN',
      'SENTRY_ENVIRONMENT',
      'SENTRY_RELEASE',
      'CURRENT_COHORT',
    ],
  },
  entry: './ssg/client.tsx',
  // Sentry 업로드용 소스맵을 생성하고 번들에는 소스맵 URL을 첨부하지 않는다.
  devtool: isProduction ? 'hidden-source-map' : undefined,
  // 타입 검사 플러그인의 디렉터리 감시에도 적용해 의존성과 생성물을 제외합니다.
  watchOptions: {
    ignored: /[\\/](?:node_modules|dist|\.git|\.tanstack|\.build)(?:[\\/]|$)/,
  },
  output: {
    path: path.resolve(__dirname, 'dist'),
    // 삭제된 라우트의 HTML이나 이전 contenthash 자산이 배포물에 남지 않게 합니다.
    clean: true,
    publicPath: '/',
    // 프로덕션에서는 캐시 무효화를 위해 contenthash를 붙입니다. 실제 파일명은 아래
    // BuildAssetsPlugin이 dist/build-assets.json으로 남기므로, ssg/render.tsx/
    // ssg/generate.ts 어디에도 "main.js"를 하드코딩하지 않습니다.
    filename: isProduction ? '[name].[contenthash:8].js' : '[name].js',
    chunkFilename: isProduction ? '[name].[contenthash:8].chunk.js' : '[name].chunk.js',
  },
  devServer: {
    port: 5173,
    open: true,
    historyApiFallback: true,
    // Document는 프레임워크가 아니라 그냥 React 컴포넌트라서, 요청마다 렌더링해 줄 서버가
    // 없는 dev server에서는 미리 만들어둔 dist/index.html(predev가 채워둔 CSR 셸)을 정적으로
    // 서빙합니다. 번들(main.js)만 dev server가 갈아끼웁니다.
    static: [
      { directory: path.resolve(__dirname, 'dist'), watch: false },
      { directory: path.resolve(__dirname, 'public'), publicPath: '/' },
    ],
  },
  plugins: [
    new ForkTsCheckerWebpackPlugin(),
    // public/은 dev server만 서빙하고, 배포는 dist만 올라갑니다. 그래서 정적 파일을 dist로 복사합니다.
    // MSW 워커는 개발에서만 쓰므로 배포물에 넣지 않습니다.
    new CopyPlugin({
      patterns: [
        {
          from: path.resolve(__dirname, 'public'),
          globOptions: { ignore: ['**/mockServiceWorker.js'] },
        },
      ],
    }),
    tanstackRouter({
      target: 'react',
      autoCodeSplitting: true,
    }),
    // 개발 환경은 style-loader로 CSS를 JS에서 <style>로 주입해 HMR을 살리고,
    // 프로덕션은 별도 .css 파일로 뽑아 브라우저가 병렬로 캐시/로드하게 합니다.
    new MiniCssExtractPlugin({
      filename: isProduction ? '[name].[contenthash:8].css' : '[name].css',
    }),
    new BuildAssetsPlugin({ importRequests: ['posthog-js'] }),
  ],
  module: {
    rules: [
      {
        test: /\.(ts|tsx|js|jsx)$/i,
        loader: 'swc-loader',
        exclude: /[\\/]node_modules[\\/]/,
      },
      {
        test: /\.css$/i,
        use: [
          isProduction ? MiniCssExtractPlugin.loader : 'style-loader',
          'css-loader',
          'postcss-loader',
        ],
      },
    ],
  },
  optimization: isProduction ? { splitChunks } : undefined,
  resolve: {
    extensions: ['.tsx', '.ts', '.jsx', '.js'],
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
};

export default () => {
  if (isProduction) {
    config.mode = 'production';
  } else {
    config.mode = 'development';
  }
  return config;
};
