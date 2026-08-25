// 번들을 나누지 않는 현재 구조에서는 client entry 스크립트 하나와, 있다면 CSS 파일
// 하나만 있으면 됩니다. 코드 스플리팅을 도입하면 이 계약을 배열 형태로 확장합니다.
export type BuildAssets = {
    script: string;
    style?: string;
};

// injectBuildAssets가 최종 HTML에 심어둔 <script id="__BUILD_ASSETS__">를 읽습니다.
// Document/App은 이 값을 직접 쓰지 않고, 필요한 곳에서만 선택적으로 사용합니다.
export function readBuildAssets(): BuildAssets {
    const element = document.getElementById('__BUILD_ASSETS__');

    if (!element || !element.textContent || element.textContent.trim() === '') {
        throw new Error('Build assets not found');
    }

    return JSON.parse(element.textContent);
}
