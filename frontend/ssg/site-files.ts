/// <reference types="node" />
import { writeFileSync } from 'node:fs';
import path from 'node:path';
import { SITE_ORIGIN } from '@/constants/site';

const escapeXml = (value: string) =>
  value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

// 검색엔진에 알릴 경로로 sitemap.xml을 만듭니다. 수정 시각(lastmod)은 정확히 알 수 없어 넣지 않습니다.
export function renderSitemap(pathnames: string[]): string {
  const urls = [...new Set(pathnames)]
    .sort()
    .map((pathname) => `  <url><loc>${escapeXml(new URL(pathname, SITE_ORIGIN).href)}</loc></url>`);
  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
    ...urls,
    '</urlset>',
    '',
  ].join('\n');
}

// 운영 배포(main)만 수집을 허용합니다. 스테이징(develop)과 로컬 빌드는 같은 내용이 검색에 잡히지 않게 모두 막습니다.
export function renderRobots(isProductionSite: boolean): string {
  if (!isProductionSite) return ['User-agent: *', 'Disallow: /', ''].join('\n');
  return [
    'User-agent: *',
    // 관리자·가입·내 정보와 글쓰기·수정 화면은 검색 결과에 나올 필요가 없습니다.
    'Disallow: /admin',
    'Disallow: /signup',
    'Disallow: /mypage',
    'Disallow: /*/new$',
    'Disallow: /*/edit$',
    '',
    `Sitemap: ${SITE_ORIGIN}/sitemap.xml`,
    '',
  ].join('\n');
}

export function writeSiteFiles(distDir: string, pathnames: string[]): void {
  // 배포 워크플로가 main 브랜치에서 빌드할 때만 운영 사이트로 봅니다(webpack.config.js의 SENTRY_ENVIRONMENT와 같은 기준).
  const isProductionSite =
    process.env.GITHUB_ACTIONS === 'true' && process.env.GITHUB_REF_NAME === 'main';

  writeFileSync(path.join(distDir, 'sitemap.xml'), renderSitemap(pathnames));
  writeFileSync(path.join(distDir, 'robots.txt'), renderRobots(isProductionSite));
  console.log(
    `[site] sitemap.xml (${new Set(pathnames).size} urls), robots.txt (${isProductionSite ? '운영: 수집 허용' : '운영 아님: 수집 막음'})`,
  );
}
