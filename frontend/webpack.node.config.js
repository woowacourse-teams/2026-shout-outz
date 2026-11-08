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
    dir: __dirname,
    template: ['.env'],
    prefix: ['API_ORIGIN', 'CURRENT_COHORT', 'POSTHOG_KEY'],
  },
  entry: {
    // SSG와 SSR 서버를 같은 설정으로 번들링
    'generate-static-site': path.resolve(__dirname, 'ssg', 'generate.ts'),
    server: path.resolve(__dirname, 'server', 'index.ts'),
  },
  output: {
    path: path.resolve(__dirname, '.build'),
    filename: '[name].cjs',
    asyncChunks: false,
    clean: true, // 이전 빌드의 생성물 제거
  },
  plugins: [
    new ForkTsCheckerWebpackPlugin(),
    tanstackRouter({
      target: 'react',
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
      '@': path.resolve(__dirname, 'src'),
    },
  },
});
