import { render, screen } from '@testing-library/react';
import type { Feed } from '@/apis/feed';
import { FeedAuthor } from '@/components/feeds/FeedAuthor';

const author: Feed['author'] = {
  userId: 1,
  handle: 'crew',
  displayName: '크루 이름',
  userType: 'WOOWACOURSE_CREW',
  cohort: 8,
  track: 'BACKEND',
  isCurrent: true,
};

it('본인의 익명 글에는 기수와 파트를 표시한다', () => {
  render(<FeedAuthor author={author} isAnonymous profileLink={false} />);

  expect(screen.getByText('8기 백엔드 크루')).toBeInTheDocument();
  expect(screen.getByText('익명으로 작성한 글입니다')).toBeInTheDocument();
  expect(screen.getByText('@crew')).toBeInTheDocument();
});

it('타인의 익명 글에는 기수와 파트를 표시하지 않는다', () => {
  render(
    <FeedAuthor
      author={{ ...author, userId: null, handle: null, displayName: null }}
      isAnonymous
      profileLink={false}
    />,
  );

  expect(screen.getByText('익명')).toBeInTheDocument();
  expect(screen.queryByText('@crew')).not.toBeInTheDocument();
  expect(screen.queryByText('8기 백엔드 크루')).not.toBeInTheDocument();
  expect(screen.queryByText('익명으로 작성한 글입니다')).not.toBeInTheDocument();
});
