import { render, screen } from '@testing-library/react';

import { FeedCard, type FeedCardProps } from '@/components/FeedCard';

const FEED: FeedCardProps = {
  author: {
    handle: 'hoik',
    displayName: '황호익',
    userType: 'WOOWACOURSE_CREW',
    track: 'BACKEND',
    cohort: 6,
    avatarImageId: null,
  },
  content: '루프 프로젝트에서 WebSocket 동기화 지연을 Redis Pub/Sub으로 개선한 과정을 공유합니다.',
  createdAt: '2026-09-15T10:00:00+09:00',
};

describe('FeedCard', () => {
  beforeEach(() => {
    jest.useFakeTimers({ now: new Date('2026-09-15T12:00:00+09:00') });
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('피드 한 건의 작성자·작성 시각·본문을 보여준다', () => {
    render(<FeedCard {...FEED} />);

    expect(screen.getByRole('article')).toBeInTheDocument();
    expect(screen.getByText('황호익 · 6기 백엔드')).toBeInTheDocument();
    expect(screen.getByText(FEED.content)).toBeInTheDocument();
  });

  it('작성 시각은 상대 표현으로 보여주고 기계가 읽을 원래 시각을 함께 둔다', () => {
    render(<FeedCard {...FEED} />);

    expect(screen.getByText('2시간 전')).toHaveAttribute('datetime', FEED.createdAt);
  });

  it('Markdown 본문은 기호 대신 서식으로 보여준다', () => {
    render(<FeedCard {...FEED} content="루프 프로젝트에 **Redis Pub/Sub**을 적용했습니다." />);

    expect(screen.getByText('Redis Pub/Sub', { selector: 'strong' })).toBeInTheDocument();
    expect(screen.queryByText(/\*\*/)).not.toBeInTheDocument();
  });
});
