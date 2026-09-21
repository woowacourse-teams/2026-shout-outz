import type { HomeBanner } from '@/types/home';
import { resolveHomeBannerLink } from '@/utils/home-banner';

const banner = (overrides: Partial<HomeBanner>): HomeBanner => ({
  bannerId: 1,
  mediaId: 21,
  imageUrl: 'https://cdn.example.com/banner',
  destinationType: 'URL',
  ...overrides,
});

describe('resolveHomeBannerLink', () => {
  describe('destinationType이 TARGET이면 대상 리소스의 상세 경로로 간다', () => {
    it.each([
      ['NEWS', '/news/7'],
      ['PROJECT', '/projects/7'],
      ['FEED', '/feeds/7'],
    ] as const)('%s는 %s', (targetType, href) => {
      expect(
        resolveHomeBannerLink(banner({ destinationType: 'TARGET', targetType, targetId: 7 })),
      ).toEqual({ kind: 'internal', href });
    });

    it('대상 유형이나 ID가 비면 이동하지 않는다', () => {
      expect(
        resolveHomeBannerLink(banner({ destinationType: 'TARGET', targetType: null, targetId: 7 })),
      ).toBeNull();
      expect(
        resolveHomeBannerLink(
          banner({ destinationType: 'TARGET', targetType: 'NEWS', targetId: null }),
        ),
      ).toBeNull();
    });
  });

  describe('destinationType이 URL이면 linkType을 따른다', () => {
    it('INTERNAL_PATH는 내부 경로로 간다', () => {
      expect(
        resolveHomeBannerLink(banner({ linkType: 'INTERNAL_PATH', linkUrl: '/projects/3001' })),
      ).toEqual({ kind: 'internal', href: '/projects/3001' });
    });

    it('EXTERNAL_URL은 외부 주소로 간다', () => {
      expect(
        resolveHomeBannerLink(banner({ linkType: 'EXTERNAL_URL', linkUrl: 'https://example.com' })),
      ).toEqual({ kind: 'external', href: 'https://example.com' });
    });

    it('linkType이 비면 주소 모양으로 짐작하지 않고 이동하지 않는다', () => {
      expect(resolveHomeBannerLink(banner({ linkUrl: '/news/1' }))).toBeNull();
      expect(resolveHomeBannerLink(banner({ linkUrl: 'https://example.com' }))).toBeNull();
    });

    it('주소가 비면 이동하지 않는다', () => {
      expect(
        resolveHomeBannerLink(banner({ linkType: 'INTERNAL_PATH', linkUrl: null })),
      ).toBeNull();
      expect(
        resolveHomeBannerLink(banner({ linkType: 'EXTERNAL_URL', linkUrl: '   ' })),
      ).toBeNull();
    });
  });

  describe('링크로 쓸 수 없는 주소는 거른다', () => {
    it('linkType이 맞아도 http(s)도 내부 경로도 아니면 이동하지 않는다', () => {
      expect(
        resolveHomeBannerLink(banner({ linkType: 'EXTERNAL_URL', linkUrl: 'javascript:alert(1)' })),
      ).toBeNull();
      expect(
        resolveHomeBannerLink(banner({ linkType: 'INTERNAL_PATH', linkUrl: '//evil.example.com' })),
      ).toBeNull();
    });

    it('linkType과 주소 모양이 어긋나면 이동하지 않는다', () => {
      expect(
        resolveHomeBannerLink(
          banner({ linkType: 'INTERNAL_PATH', linkUrl: 'https://example.com' }),
        ),
      ).toBeNull();
      expect(
        resolveHomeBannerLink(banner({ linkType: 'EXTERNAL_URL', linkUrl: '/projects/1' })),
      ).toBeNull();
    });
  });
});
