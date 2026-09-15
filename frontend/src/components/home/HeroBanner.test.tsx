import { render, screen } from '@testing-library/react';

import { HeroBanner, type HeroBannerProps } from '@/components/home/HeroBanner';

const BANNER: HeroBannerProps = {
  thumbnailUrl: 'https://cdn.example.com/banners/loop-thumbnail.webp',
  originalUrl: 'https://cdn.example.com/banners/loop-original.webp',
  alt: '금주의 추천 프로젝트 루프(Loop) - 스프린트 회고와 액션 아이템을 하나로 묶은 협업 도구',
  href: '/projects/1',
};

describe('HeroBanner', () => {
  it('배너 이미지를 대체 텍스트와 함께 보여준다', () => {
    render(<HeroBanner {...BANNER} />);

    expect(screen.getByRole('img', { name: BANNER.alt })).toBeInTheDocument();
  });

  it('배너 전체가 대체 텍스트를 이름으로 가진 링크다', () => {
    render(<HeroBanner {...BANNER} />);

    expect(screen.getByRole('link', { name: BANNER.alt })).toHaveAttribute('href', BANNER.href);
  });
});
