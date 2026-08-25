import type { BuildAssets } from '../buildAssets';

function insertBefore(html: string, marker: string, insertion: string, what: string): string {
    const index = html.indexOf(marker);

    if (index === -1) {
        throw new Error(`injectBuildAssets: ${what}를 넣을 "${marker}"를 HTML에서 찾지 못했습니다.`);
    }

    return `${html.slice(0, index)}${insertion}${html.slice(index)}`;
}

// CSR용 dist/index.html과 SSG 결과물이 공유하는 단 하나의 asset 후처리 규칙입니다.
// Document 안에는 실제 asset이나 asset용 React 컴포넌트를 두지 않고, React가 만든 HTML
// 문자열에 이 함수로 직접 스크립트/링크를 끼워 넣습니다.
export function injectBuildAssets(html: string, assets: BuildAssets): string {
    let result = html;

    // CSS는 렌더 차단 리소스라 <head>에 둬야 FOUC 없이 보여줍니다.
    if (assets.style) {
        result = insertBefore(result, '</head>', `<link rel="stylesheet" href="${assets.style}">`, '스타일시트 링크');
    }

    const serializedAssets = JSON.stringify(assets).replace(/</g, '\\u003c');
    const bodyAssets = `<script id="__BUILD_ASSETS__" type="application/json">${serializedAssets}</script><script type="module" src="${assets.script}"></script>`;

    result = insertBefore(result, '</body>', bodyAssets, 'client entry 스크립트');

    return result;
}
