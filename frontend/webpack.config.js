import path from 'node:path';
import { fileURLToPath } from 'node:url';
import 'webpack-dev-server';
import ForkTsCheckerWebpackPlugin from 'fork-ts-checker-webpack-plugin';
import MiniCssExtractPlugin from 'mini-css-extract-plugin';
import { WebpackManifestPlugin } from 'webpack-manifest-plugin';
import { tanstackRouter } from '@tanstack/router-plugin/webpack';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const isProduction = process.env.NODE_ENV === 'production';

/** @type {import("webpack").Configuration} */
const config = {
    entry: './ssg/client.tsx',
    output: {
        path: path.resolve(__dirname, 'dist'),
        // 삭제된 라우트의 HTML이나 이전 contenthash 자산이 배포물에 남지 않게 합니다.
        clean: true,
        publicPath: '/',
        // 프로덕션에서는 캐시 무효화를 위해 contenthash를 붙입니다. 실제 파일명은 아래
        // WebpackManifestPlugin이 dist/build-assets.json으로 남기므로, ssg/render.tsx/
        // ssg/generate.ts 어디에도 "main.js"를 하드코딩하지 않습니다.
        filename: isProduction ? '[name].[contenthash:8].js' : '[name].js',
        chunkFilename: isProduction ? '[name].[contenthash:8].chunk.js' : '[name].chunk.js',
    },
    devServer: {
        open: true,
        historyApiFallback: true,
        // Document는 프레임워크가 아니라 그냥 React 컴포넌트라서, 요청마다 렌더링해 줄 서버가
        // 없는 dev server에서는 미리 만들어둔 dist/index.html(predev가 채워둔 CSR 셸)을 정적으로
        // 서빙합니다. 번들(main.js)만 dev server가 갈아끼웁니다.
        static: {
            directory: path.resolve(__dirname, 'dist'),
        },
    },
    plugins: [
        new ForkTsCheckerWebpackPlugin(),
        tanstackRouter({
            target: 'react',
            autoCodeSplitting: true,
        }),
        // 개발 환경은 style-loader로 CSS를 JS에서 <style>로 주입해 HMR을 살리고,
        // 프로덕션은 별도 .css 파일로 뽑아 브라우저가 병렬로 캐시/로드하게 합니다.
        new MiniCssExtractPlugin({
            filename: isProduction ? '[name].[contenthash:8].css' : '[name].css',
        }),
        new WebpackManifestPlugin({
            fileName: 'build-assets.json',
            generate: (_seed, _files, entrypoints) => {
                const mainFiles = entrypoints.main ?? [];
                const script = mainFiles.find((file) => file.endsWith('.js'));
                const style = mainFiles.find((file) => file.endsWith('.css'));

                if (!script) {
                    throw new Error('WebpackManifestPlugin: main entry script를 찾지 못했습니다.');
                }

                return { script: `/${script}`, ...(style ? { style: `/${style}` } : {}) };
            },
        }),
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
                use: [isProduction ? MiniCssExtractPlugin.loader : 'style-loader', 'css-loader', 'postcss-loader'],
            },
        ],
    },
    resolve: {
        extensions: ['.tsx', '.ts', '.jsx', '.js'],
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
