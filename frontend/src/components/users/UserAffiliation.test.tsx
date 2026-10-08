import { render, screen } from '@testing-library/react';
import { UserAffiliation } from '@/components/users/UserAffiliation';

test.each([
  [
    { userType: 'WOOWACOURSE_CREW', cohort: 8, track: 'BACKEND', isCurrent: true },
    '8기 백엔드 크루',
  ],
  [
    { userType: 'WOOWACOURSE_CREW', cohort: 7, track: 'FRONTEND', isCurrent: false },
    '7기 프론트엔드 크루',
  ],
  [
    { userType: 'WOOWACOURSE_CREW', cohort: 8, track: 'BACKEND', isCurrent: true, anonymous: true },
    '크루',
  ],
  [{ userType: 'WOOWACOURSE_CREW', isCurrent: false, anonymous: true }, '수료생'],
  [{ userType: 'WOOWACOURSE_CREW' }, '크루'],
  [{ userType: 'WOOWACOURSE_COACH', isCurrent: false }, '코치'],
])('소속 정보 %j를 문구로 표시하고 인증 배지는 표시하지 않는다', (user, label) => {
  render(<UserAffiliation {...user} />);
  expect(screen.getByText(label)).toBeInTheDocument();
  expect(screen.queryByRole('img', { name: '우아한테크코스 소속 인증' })).not.toBeInTheDocument();
});

test.each(['GENERAL', undefined])(
  '소속이 없는 회원 %s는 배지와 문구를 표시하지 않는다',
  (userType) => {
    const { container } = render(
      <UserAffiliation userType={userType} cohort={8} track="BACKEND" isCurrent={false} />,
    );
    expect(container).toBeEmptyDOMElement();
  },
);
