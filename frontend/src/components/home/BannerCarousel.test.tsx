import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { BannerCarousel } from '@/components/home/BannerCarousel';
import { HomeBannerSection } from '@/components/home/HomeBannerSection';
import { homeBannersQueryOptions } from '@/api/home';
import type { HomeBanner } from '@/types/home';

const banners: HomeBanner[] = [1, 2, 3].map((id) => ({
  bannerId: id,
  mediaId: id,
  imageUrl: `https://cdn.example.com/banner-${id}.webp`,
  destinationType: 'URL',
  linkType: 'EXTERNAL_URL',
  linkUrl: `https://example.com/${id}`,
}));

const mockMotionPreference = (matches = false) => {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    writable: true,
    value: jest.fn().mockReturnValue({
      matches,
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
    }),
  });
};

const expectBanner = (number: number) => {
  expect(screen.getByRole('link', { name: '홈 배너' })).toHaveAttribute(
    'href',
    `https://example.com/${number}`,
  );
  expect(screen.getByText(`${number} / 3`)).toBeInTheDocument();
  expect(screen.getByRole('button', { name: `${number}번 배너 보기` })).toHaveAttribute(
    'aria-current',
    'true',
  );
};

beforeEach(() => {
  jest.useFakeTimers();
  mockMotionPreference();
});

afterEach(() => {
  jest.useRealTimers();
});

test('배너 개수와 현재 위치를 표시하고 버튼과 위치 막대로 순환 이동한다', async () => {
  const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
  render(<BannerCarousel banners={banners} />);
  expectBanner(1);

  await user.click(screen.getByRole('button', { name: '이전 배너' }));
  expectBanner(3);
  await user.click(screen.getByRole('button', { name: '다음 배너' }));
  expectBanner(1);
  await user.click(screen.getByRole('button', { name: '2번 배너 보기' }));
  expectBanner(2);
  expect(screen.getAllByRole('link')).toHaveLength(1);
});

test('5초마다 전환하고 마우스를 올리거나 초점이 들어오면 멈춘다', () => {
  render(<BannerCarousel banners={banners} />);
  act(() => jest.advanceTimersByTime(5_000));
  expectBanner(2);

  const region = screen.getByRole('region', { name: '홈 배너 모음' });
  fireEvent.mouseEnter(region);
  act(() => jest.advanceTimersByTime(10_000));
  expectBanner(2);
  fireEvent.mouseLeave(region);
  act(() => jest.advanceTimersByTime(5_000));
  expectBanner(3);

  fireEvent.focus(screen.getByRole('button', { name: '다음 배너' }));
  act(() => jest.advanceTimersByTime(10_000));
  expectBanner(3);
});

test('일시정지 후 영역을 벗어나도 멈춰 있고 재생하면 다시 전환한다', async () => {
  const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
  render(<BannerCarousel banners={banners} />);
  await user.click(screen.getByRole('button', { name: '배너 자동 재생 일시정지' }));
  const region = screen.getByRole('region', { name: '홈 배너 모음' });
  fireEvent.mouseLeave(region);
  fireEvent.blur(screen.getByRole('button', { name: '배너 자동 재생' }), {
    relatedTarget: document.body,
  });
  act(() => jest.advanceTimersByTime(10_000));
  expectBanner(1);

  await user.click(screen.getByRole('button', { name: '배너 자동 재생' }));
  fireEvent.mouseLeave(region);
  fireEvent.blur(screen.getByRole('button', { name: '배너 자동 재생 일시정지' }), {
    relatedTarget: document.body,
  });
  act(() => jest.advanceTimersByTime(5_000));
  expectBanner(2);
});

test('움직임 줄이기 설정이면 자동 전환하지 않는다', () => {
  mockMotionPreference(true);
  render(<BannerCarousel banners={banners} />);
  act(() => jest.advanceTimersByTime(10_000));
  expectBanner(1);
  expect(screen.queryByRole('button', { name: '배너 자동 재생 일시정지' })).not.toBeInTheDocument();
});

test('조회한 배너 목록이 바뀌면 새 목록의 첫 배너를 표시한다', async () => {
  const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
  const client = new QueryClient({ defaultOptions: { queries: { staleTime: Infinity } } });
  const { queryKey } = homeBannersQueryOptions();
  client.setQueryData(queryKey, banners);
  render(
    <QueryClientProvider client={client}>
      <HomeBannerSection />
    </QueryClientProvider>,
  );
  await user.click(screen.getByRole('button', { name: '다음 배너' }));
  expectBanner(2);

  await act(async () => {
    client.setQueryData(queryKey, [banners[2]!, banners[0]!]);
    await jest.advanceTimersByTimeAsync(0);
  });
  expect(screen.getByRole('link', { name: '홈 배너' })).toHaveAttribute(
    'href',
    'https://example.com/3',
  );
  expect(screen.getByText('1 / 2')).toBeInTheDocument();

  await act(async () => {
    client.setQueryData(queryKey, []);
    await jest.advanceTimersByTimeAsync(0);
  });
  expect(screen.queryByRole('region', { name: '홈 배너 모음' })).not.toBeInTheDocument();
  client.clear();
});
