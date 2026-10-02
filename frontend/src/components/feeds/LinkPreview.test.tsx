import { render, screen } from '@testing-library/react';

import { LinkPreview } from '@/components/feeds/LinkPreview';

const preview = {
  url: 'https://dohyun.log/redis-fanout',
  title: 'Redis로 메시지 팬아웃하기',
  description: '서버 여러 대에 메시지를 고르게 퍼뜨린 방법을 정리했습니다.',
  siteName: 'dohyun.log',
};

describe('LinkPreview', () => {
  it('기본 크기는 제목과 설명을 모두 보여준다', () => {
    render(<LinkPreview {...preview} />);

    expect(screen.getByText(preview.title)).toBeInTheDocument();
    expect(screen.getByText(preview.description)).toBeInTheDocument();
  });

  it('작은 크기는 설명을 빼고 제목과 사이트만 보여준다', () => {
    render(<LinkPreview {...preview} size="sm" />);

    expect(screen.getByRole('link', { name: `${preview.title} 링크 열기` })).toHaveAttribute(
      'href',
      preview.url,
    );
    expect(screen.getByText('dohyun.log')).toBeInTheDocument();
    expect(screen.queryByText(preview.description)).not.toBeInTheDocument();
  });
});
