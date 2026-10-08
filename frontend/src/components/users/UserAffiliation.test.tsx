import { render, screen } from '@testing-library/react';
import { UserAffiliation } from '@/components/users/UserAffiliation';

test.each([
  [
    { userType: 'WOOWACOURSE_CREW', cohort: 8, track: 'BACKEND', isCurrent: true },
    '우아한테크코스 8기 백엔드',
  ],
  [
    { userType: 'WOOWACOURSE_CREW', cohort: 7, track: 'FRONTEND', isCurrent: false },
    '우아한테크코스 7기 프론트엔드',
  ],
  [
    { userType: 'WOOWACOURSE_CREW', cohort: 8, track: 'BACKEND', isCurrent: true, anonymous: true },
    '우아한테크코스 크루',
  ],
  [{ userType: 'WOOWACOURSE_CREW', isCurrent: false, anonymous: true }, '우아한테크코스 수료생'],
  [{ userType: 'WOOWACOURSE_CREW' }, '우아한테크코스 크루'],
  [{ userType: 'WOOWACOURSE_COACH', isCurrent: false }, '우아한테크코스 코치'],
])('소속 정보 %j를 문구와 인증 배지로 표시한다', (user, label) => {
  render(<UserAffiliation {...user} />);
  expect(screen.getByText(label)).toBeInTheDocument();
  expect(screen.getByRole('img', { name: '우아한테크코스 소속 인증' })).toBeInTheDocument();
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
