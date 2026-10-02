/** @jest-environment-options {"url":"http://localhost:5173"} */
import { render, screen } from '@testing-library/react';
import { DevelopmentBanner } from '@/components/DevelopmentBanner';

test('localhost에서는 개발 안내를 표시한다', () => {
  render(<DevelopmentBanner />);
  expect(screen.getByRole('complementary', { name: '개발 환경 안내' })).toBeInTheDocument();
});
