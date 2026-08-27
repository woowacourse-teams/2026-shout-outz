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
  entry: path.resolve(__dirname, 'generate.ts'),
  output: {
    path: path.resolve(__dirname, '.build'),
    // filename은 초기 entry, chunkFilename은 TanStack Router가 만든 라우트 async chunk에 적용됩니다.
    filename: 'generate-static-site.cjs',
    chunkFilename: '[name].cjs',
    // 라우트가 삭제되거나 chunk 이름이 바뀌 때 이전 생성물이 남지 않게 합니다.
    clean: true,
  },
  plugins: [
    new ForkTsCheckerWebpackPlugin(),
    tanstackRouter({
      target: 'react',
      autoCodeSplitting: true,
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
  },
});
