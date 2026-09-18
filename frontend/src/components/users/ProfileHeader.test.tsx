import { render, screen } from '@testing-library/react';

import { ProfileHeader, type ProfileHeaderProps } from '@/components/users/ProfileHeader';

const PROFILE: ProfileHeaderProps = {
  displayName: '정우진',
  cohort: 6,
  track: 'BACKEND',
  bio: '대규모 트래픽 분산 처리와 데이터 정합성에 집착하는 백엔드 개발자입니다.',
  githubProfileUrl: 'https://github.com/woojin-dev',
  blogUrl: 'https://woojin.log',
};

describe('ProfileHeader', () => {
  it('이름과 소속, 소개를 보여준다', () => {
    render(<ProfileHeader {...PROFILE} />);

    expect(screen.getByRole('heading', { name: '정우진' })).toBeInTheDocument();
    expect(screen.getByText('6기 백엔드')).toBeInTheDocument();
    expect(screen.getByText(PROFILE.bio!)).toBeInTheDocument();
  });

  it('GitHub·블로그 링크를 주소로 연결한다', () => {
    render(<ProfileHeader {...PROFILE} />);

    expect(screen.getByRole('link', { name: 'GitHub' })).toHaveAttribute(
      'href',
      PROFILE.githubProfileUrl,
    );
    expect(screen.getByRole('link', { name: '블로그' })).toHaveAttribute('href', PROFILE.blogUrl);
  });

  it('소개와 링크가 없으면 그 자리를 그리지 않는다', () => {
    render(<ProfileHeader {...PROFILE} bio={null} githubProfileUrl={null} blogUrl={null} />);

    expect(screen.getByRole('heading', { name: '정우진' })).toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    expect(screen.queryByText(PROFILE.bio!)).not.toBeInTheDocument();
  });

  it('소속을 알 수 없으면 배지를 그리지 않는다', () => {
    render(<ProfileHeader {...PROFILE} cohort={null} track={null} />);

    expect(screen.queryByText(/기 백엔드/)).not.toBeInTheDocument();
  });
});
