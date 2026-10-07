import type { PropsWithChildren } from 'react';

// index.html을 대체하는 문서 전체(<html>/<head>/<body>)의 source of truth입니다.
// 페이지별 <title>/<meta>는 여기서 선언하지 않고, 그걸 필요로 하는 컴포넌트가 직접 렌더링합니다.
export function Document({ children }: PropsWithChildren) {
  return (
    <html lang="ko">
      <head>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        {/* 파비콘 원본은 public/favicon/에 있고, 빌드 때 dist/favicon/으로 복사된다. */}
        <link rel="icon" href="/favicon/favicon.ico" sizes="any" />
        <link rel="icon" type="image/png" sizes="16x16" href="/favicon/favicon-16x16.png" />
        <link rel="icon" type="image/png" sizes="32x32" href="/favicon/favicon-32x32.png" />
        <link rel="icon" type="image/png" sizes="48x48" href="/favicon/favicon-48x48.png" />
        <link rel="apple-touch-icon" sizes="180x180" href="/favicon/apple-touch-icon.png" />
        <link rel="manifest" href="/favicon/site.webmanifest" />
        {/* 링크 공유 미리보기. 크롤러는 JS를 실행하지 않아 모든 페이지에 공통으로 넣고, 이미지는 절대 주소여야 한다. */}
        <meta property="og:type" content="website" />
        <meta property="og:site_name" content="shout-outz" />
        <meta property="og:title" content="shout-outz" />
        <meta
          property="og:description"
          content="궁금한 건 편하게 묻고, 서로의 이야기에 응원을 보내 보세요."
        />
        <meta property="og:image" content="https://shout-ou.tz/og-image.jpg" />
        <meta property="og:image:width" content="1200" />
        <meta property="og:image:height" content="630" />
        <meta property="og:image:alt" content="메가폰으로 응원을 외치는 shout-outz 캐릭터" />
        <meta name="twitter:card" content="summary_large_image" />
      </head>
      <body>{children}</body>
    </html>
  );
}
