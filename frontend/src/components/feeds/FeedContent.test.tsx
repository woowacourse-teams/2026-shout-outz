import { render, screen } from '@testing-library/react';

import { FeedContent } from '@/components/feeds/FeedContent';
import { type Feed } from '@/apis/feed';

const feed = (overrides: Partial<Feed> = {}): Feed => ({
  feedId: 1,
  title: 'Redis Pub/Sub 동기화 개선기',
  content: '캐시 무효화와 메시지 순서를 함께 고민했어요.',
  author: {
    handle: 'crew0',
    displayName: '정우진',
    userType: 'WOOWACOURSE_CREW',
    track: 'BACKEND',
    cohort: 6,
    avatarUrl: null,
  },
  categories: [],
  media: [],
  createdAt: '2026-09-14T00:00:00Z',
  updatedAt: '2026-09-14T00:00:00Z',
  ...overrides,
});

describe('FeedContent', () => {
  it('제목을 본문 위에 제목으로 보여준다', () => {
    render(<FeedContent feed={feed()} />);

    expect(
      screen.getByRole('heading', { name: 'Redis Pub/Sub 동기화 개선기' }),
    ).toBeInTheDocument();
    expect(screen.getByText('캐시 무효화와 메시지 순서를 함께 고민했어요.')).toBeInTheDocument();
  });

  it('카드에서는 h3, 상세에서는 h2로 그린다', () => {
    const { rerender } = render(<FeedContent feed={feed()} />);
    expect(screen.getByRole('heading', { level: 3 })).toBeInTheDocument();

    rerender(<FeedContent feed={feed()} titleAs="h2" />);
    expect(screen.getByRole('heading', { level: 2 })).toBeInTheDocument();
  });
});
