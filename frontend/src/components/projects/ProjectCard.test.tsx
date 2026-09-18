import { render, screen, within } from '@testing-library/react';

import { ProjectCard, type ProjectCardProps } from '@/components/projects/ProjectCard';

const PROJECT: ProjectCardProps = {
  title: '모아모아 (MoaMoa)',
  tagline: '사진 한 장으로 영수증 내역을 자동 분리하고 맞춤 정산하는 웹 서비스',
  cohort: 6,
  likeCount: 184,
  commentCount: 14,
  techTags: [
    { id: 1, displayName: 'React' },
    { id: 2, displayName: 'Spring' },
  ],
  members: [
    { userId: 7, displayName: '박다혜' },
    { userId: 8, displayName: '김도현' },
  ],
};

describe('ProjectCard', () => {
  it('기수·제목·한 줄 소개를 보여준다', () => {
    render(<ProjectCard {...PROJECT} />);

    expect(screen.getByText('우아한테크코스 6기')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: PROJECT.title })).toBeInTheDocument();
    expect(screen.getByText(PROJECT.tagline)).toBeInTheDocument();
  });

  it('기술 스택을 모두 보여준다', () => {
    render(<ProjectCard {...PROJECT} />);

    const techTags = screen.getByRole('list', { name: '기술 스택' });

    expect(within(techTags).getAllByRole('listitem')).toHaveLength(2);
    expect(within(techTags).getByText('React')).toBeInTheDocument();
    expect(within(techTags).getByText('Spring')).toBeInTheDocument();
  });

  it('참여자와 좋아요·댓글 수를 보여준다', () => {
    render(<ProjectCard {...PROJECT} />);

    expect(screen.getByRole('img', { name: '박다혜' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: '김도현' })).toBeInTheDocument();
    expect(screen.getByLabelText('좋아요 수')).toHaveTextContent('184');
    expect(screen.getByLabelText('댓글 수')).toHaveTextContent('14');
  });

  it('기수를 모르면 그 줄을 그리지 않는다', () => {
    render(<ProjectCard {...PROJECT} cohort={null} />);

    expect(screen.queryByText(/우아한테크코스/)).not.toBeInTheDocument();
  });

  it('기술 스택이 없으면 그 목록을 그리지 않는다', () => {
    render(<ProjectCard {...PROJECT} techTags={[]} />);

    expect(screen.queryByRole('list', { name: '기술 스택' })).not.toBeInTheDocument();
  });

  // 썸네일 이미지는 아직 붙이지 않는다. 응답이 thumbnailMediaId만 주기 때문이다.
  it('썸네일 자리에 제목을 대신 보여준다', () => {
    render(<ProjectCard {...PROJECT} />);

    expect(screen.getAllByText(PROJECT.title).length).toBeGreaterThan(1);
  });
});
