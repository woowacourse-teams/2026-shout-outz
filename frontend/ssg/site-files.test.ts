import { renderRobots, renderSitemap } from './site-files';

describe('renderSitemap', () => {
  it('경로를 운영 주소로 바꿔 중복 없이 정렬해 넣는다', () => {
    const xml = renderSitemap(['/projects/@shout-outz/intro', '/', '/projects/@shout-outz', '/']);

    expect(xml.match(/<loc>(.*?)<\/loc>/g)).toEqual([
      '<loc>https://shout-ou.tz/</loc>',
      '<loc>https://shout-ou.tz/projects/@shout-outz</loc>',
      '<loc>https://shout-ou.tz/projects/@shout-outz/intro</loc>',
    ]);
    expect(xml.startsWith('<?xml version="1.0" encoding="UTF-8"?>')).toBe(true);
  });
});

describe('renderRobots', () => {
  it('운영 사이트는 수집을 허용하고 sitemap 주소를 알린다', () => {
    const robots = renderRobots(true);

    expect(robots).toContain('Disallow: /admin');
    expect(robots).not.toMatch(/^Disallow: \/$/m);
    expect(robots).toContain('Sitemap: https://shout-ou.tz/sitemap.xml');
  });

  it('운영이 아니면 모두 막는다', () => {
    expect(renderRobots(false)).toBe('User-agent: *\nDisallow: /\n');
  });
});
