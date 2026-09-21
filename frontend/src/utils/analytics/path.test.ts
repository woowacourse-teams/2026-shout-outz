import { toPathPattern } from '@/utils/analytics/path';

describe('toPathPattern', () => {
  it.each([
    ['/users/woojin', '/users/:handle'],
    ['/feeds/101', '/feeds/:feedId'],
    ['/feeds/101/edit', '/feeds/:feedId/edit'],
    ['/projects/12', '/projects/:projectId'],
    ['/news/3', '/news/:newsId'],
  ])('%s의 식별자를 패턴으로 바꾼다', (path, expected) => {
    expect(toPathPattern(path)).toBe(expected);
  });

  it.each(['/', '/feeds', '/projects/new', '/users', '/signup'])(
    '식별자가 없는 %s는 그대로 둔다',
    (path) => {
      expect(toPathPattern(path)).toBe(path);
    },
  );

  it('끝의 슬래시를 정리해 같은 화면을 하나로 센다', () => {
    expect(toPathPattern('/projects/')).toBe('/projects');
    expect(toPathPattern('/')).toBe('/');
  });

  it('화면 상태를 나타내는 쿼리만 남긴다', () => {
    expect(toPathPattern('/users/woojin?tab=feeds')).toBe('/users/:handle?tab=feeds');
    expect(toPathPattern('/feeds?sort=POPULAR')).toBe('/feeds?sort=POPULAR');
    expect(toPathPattern('/feeds?sort=LATEST&utm_source=slack')).toBe('/feeds?sort=LATEST');
    expect(toPathPattern('/news?type=EVENT&token=secret')).toBe('/news?type=EVENT');
  });
});
