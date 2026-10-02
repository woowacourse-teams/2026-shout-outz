/** @jest-environment-options {"url":"https://shout-ou.tz"} */
import { render, screen } from '@testing-library/react';
import { DevelopmentBanner } from '@/components/DevelopmentBanner';

test('운영 도메인에서는 개발 안내를 표시하지 않는다', () => {
  render(<DevelopmentBanner />);
  expect(screen.queryByRole('complementary', { name: '개발 환경 안내' })).not.toBeInTheDocument();
});
