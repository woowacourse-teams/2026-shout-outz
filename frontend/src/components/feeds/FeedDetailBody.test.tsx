import { render, screen } from '@testing-library/react';

import { FeedDetailBody } from '@/components/feeds/FeedDetailBody';
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

describe('FeedDetailBody', () => {
  it('제목을 본문 위에 h2로 보여준다', () => {
    render(<FeedDetailBody feed={feed()} />);

    expect(
      screen.getByRole('heading', { level: 2, name: 'Redis Pub/Sub 동기화 개선기' }),
    ).toBeInTheDocument();
    expect(screen.getByText('캐시 무효화와 메시지 순서를 함께 고민했어요.')).toBeInTheDocument();
  });

  it('첨부 이미지를 모두 보여준다', () => {
    const media = [1, 2, 3].map((order) => ({
      mediaId: order,
      displayOrder: order,
      url: `https://cdn.test/${order}.png`,
    }));
    render(<FeedDetailBody feed={feed({ media })} />);

    expect(screen.getAllByRole('img', { name: '피드 첨부 이미지' })).toHaveLength(3);
  });

  it('본문의 코드 블록을 보여준다', () => {
    render(<FeedDetailBody feed={feed({ content: '설정\n\n```ts\nconst retry = 3;\n```' })} />);

    expect(screen.getByText('const retry = 3;')).toBeInTheDocument();
  });
});
