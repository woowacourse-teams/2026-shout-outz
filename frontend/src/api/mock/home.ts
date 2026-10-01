import type { HomeBanner } from '@/types/home';

/** 외부 이미지 서버 없이 배너 순서를 구분할 수 있는 목 이미지. */
const bannerImage = (number: string, title: string, description: string, color: string) =>
  `data:image/svg+xml;charset=utf-8,${encodeURIComponent(`
    <svg xmlns="http://www.w3.org/2000/svg" width="1200" height="400" viewBox="0 0 1200 400">
      <rect width="1200" height="400" fill="${color}" />
      <circle cx="160" cy="80" r="180" fill="white" opacity=".08" />
      <circle cx="1060" cy="360" r="220" fill="white" opacity=".08" />
      <g fill="white" text-anchor="middle" font-family="sans-serif">
        <text x="600" y="120" font-size="22">BANNER ${number}</text>
        <text x="600" y="205" font-size="48" font-weight="700">${title}</text>
        <text x="600" y="255" font-size="22">${description}</text>
      </g>
    </svg>
  `)}`;

export const homeBanners: HomeBanner[] = [
  {
    bannerId: 100,
    mediaId: 21,
    imageUrl: bannerImage('01', 'PROJECTS', '크루의 프로젝트를 만나보세요', '#2563eb'),
    destinationType: 'URL',
    linkType: 'INTERNAL_PATH',
    linkUrl: '/projects/@dropit',
  },
  {
    bannerId: 101,
    mediaId: 22,
    imageUrl: bannerImage('02', 'FEEDS', '함께 나누는 개발 이야기', '#047857'),
    destinationType: 'URL',
    linkType: 'INTERNAL_PATH',
    linkUrl: '/community',
  },
  {
    bannerId: 102,
    mediaId: 23,
    imageUrl: bannerImage('03', 'NEWS', '새로운 소식을 확인해 보세요', '#b45309'),
    destinationType: 'URL',
    linkType: 'INTERNAL_PATH',
    linkUrl: '/news',
  },
];
