import path from 'node:path';
import { fileURLToPath } from 'node:url';
import ForkTsCheckerWebpackPlugin from 'fork-ts-checker-webpack-plugin';
import { tanstackRouter } from '@tanstack/router-plugin/webpack';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/** @type {import("webpack").Configuration} */
export default {
    mode: 'development',
    target: 'node',
    // 실제 실행 진입점은 src/entry-ssg.tsx(renderCsrShell/renderPage만 export)를 가져다 쓰는
    // SSG 빌드 스크립트입니다. entry-ssg.tsx 자체는 이 번들의 entry가 아닙니다.
    entry: './src/ssg/generateStaticSite.ts',
    output: {
        path: path.resolve(__dirname, 'dist-server'),
        filename: 'generate-static-site.cjs',
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
};
