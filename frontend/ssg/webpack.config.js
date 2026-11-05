import path from 'node:path';
import { fileURLToPath } from 'node:url';
import ForkTsCheckerWebpackPlugin from 'fork-ts-checker-webpack-plugin';
import { tanstackRouter } from '@tanstack/router-plugin/webpack';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/** @type {import("webpack").ConfigurationFactory} */
export default (_env, argv) => ({
  mode: argv.mode ?? 'development',
  target: 'node',
  dotenv: {
    dir: path.resolve(__dirname, '..'),
    template: ['.env'],
    prefix: ['API_ORIGIN', 'CURRENT_COHORT', 'POSTHOG_KEY'],
  },
  // 빌드 때 HTML 파일을 만드는 SSG 생성기와, 요청마다 HTML을 응답하는 SSR 서버를 같은 설정으로 번들합니다.
  entry: {
    'generate-static-site': path.resolve(__dirname, 'generate.ts'),
    server: path.resolve(__dirname, '..', 'server', 'index.ts'),
  },
  output: {
    path: path.resolve(__dirname, '..', '.build'),
    filename: '[name].cjs',
    // 서버는 디스크에서 모듈을 읽어 메모리에 두므로 청크를 나눠 받을 이유가 없습니다.
    // 동적 import(posthog-js 등)까지 번들에 넣어 entry마다 파일 하나만 남깁니다.
    asyncChunks: false,
    // 이전 빌드의 생성물이 남지 않게 합니다.
    clean: true,
  },
  plugins: [
    new ForkTsCheckerWebpackPlugin(),
    tanstackRouter({
      target: 'react',
      // 라우트 청크 분리는 브라우저 다운로드를 줄이려는 것이라 서버 번들에서는 끕니다.
      // 렌더된 HTML은 분리할 때와 같습니다(브라우저 번들은 webpack.config.js에서 그대로 분리합니다).
      autoCodeSplitting: false,
    }),
  ],
  module: {
    rules: [
      {
        test: /\.(ts|tsx|js|jsx)$/i,
        loader: 'swc-loader',
        exclude: /[\\/]node_modules[\\/]/,
      },
    ],
  },
  resolve: {
    extensions: ['.tsx', '.ts', '.jsx', '.js'],
    alias: {
      '@': path.resolve(__dirname, '..', 'src'),
    },
  },
});
