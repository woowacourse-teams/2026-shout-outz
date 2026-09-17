import { render, screen } from '@testing-library/react';

import { ProjectCard } from '@/components/projects/ProjectCard';

const PROJECT = {
  title: '모아모아 (MoaMoa)',
  tagline: '사진 한 장으로 영수증 내역을 자동 분리하고 맞춤 정산하는 웹 서비스',
};

describe('ProjectCard', () => {
  it('제목과 한 줄 소개를 보여준다', () => {
    render(<ProjectCard {...PROJECT} />);

    expect(screen.getByRole('heading', { name: PROJECT.title })).toBeInTheDocument();
    expect(screen.getByText(PROJECT.tagline)).toBeInTheDocument();
  });

  it('썸네일이 있으면 이미지로 보여준다', () => {
    render(<ProjectCard {...PROJECT} thumbnailUrl="https://cdn.example.com/moamoa.png" />);

    expect(screen.getByRole('img')).toHaveAttribute('src', 'https://cdn.example.com/moamoa.png');
  });

  it('썸네일이 없으면 제목을 대신 보여준다', () => {
    render(<ProjectCard {...PROJECT} thumbnailUrl={null} />);

    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(screen.getAllByText(PROJECT.title).length).toBeGreaterThan(0);
  });

  it('meta를 주면 제목 위에 보여준다', () => {
    render(<ProjectCard {...PROJECT} meta="우아한테크코스 6기" />);

    expect(screen.getByText('우아한테크코스 6기')).toBeInTheDocument();
  });

  it('children으로 받은 내용을 카드 안에 보여준다', () => {
    render(
      <ProjectCard {...PROJECT}>
        <span>Spring Boot</span>
      </ProjectCard>,
    );

    expect(screen.getByText('Spring Boot')).toBeInTheDocument();
  });
});
