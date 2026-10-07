/** @jest-environment-options {"url":"https://staging.shout-ou.tz"} */
import { render, screen } from '@testing-library/react';
import { DevelopmentBanner } from '@/components/DevelopmentBanner';

test('staging 도메인에서는 운영 사이트 링크와 안내를 표시한다', () => {
  render(<DevelopmentBanner />);
  expect(screen.getByRole('complementary', { name: '개발 환경 안내' })).toHaveTextContent(
    'shout-ou.tz를 이용해 주세요.',
  );
  expect(screen.getByRole('link', { name: 'shout-ou.tz' })).toHaveAttribute(
    'href',
    'https://shout-ou.tz',
  );
});
