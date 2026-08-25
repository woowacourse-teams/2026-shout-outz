import type { PropsWithChildren } from 'react';

// index.html을 대체하는 문서 전체(<html>/<head>/<body>)의 source of truth입니다.
// 페이지별 <title>/<meta>는 여기서 선언하지 않고, 그걸 필요로 하는 컴포넌트가 직접 렌더링합니다.
export function Document({ children }: PropsWithChildren) {
    return (
        <html lang="ko">
            <head>
                <meta charSet="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
            </head>
            <body>{children}</body>
        </html>
    );
}
