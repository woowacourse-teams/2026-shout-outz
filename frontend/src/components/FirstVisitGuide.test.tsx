import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FirstVisitGuide } from '@/components/FirstVisitGuide';

const key = 'shoutoutz:about-guide-seen';
beforeEach(() => localStorage.clear());
afterEach(() => jest.restoreAllMocks());

it('첫 방문에 안내하고 CTA로 소개 페이지 이동을 요청하며 재방문에는 숨긴다', async () => {
  const onExplore = jest.fn();
  const { unmount } = render(<FirstVisitGuide mode="offer" onExplore={onExplore} />);
  expect(screen.getByRole('dialog', { name: '샤라웃을 소개할게요' })).toBeInTheDocument();
  expect(localStorage.getItem(key)).toBe('true');
  await userEvent.click(screen.getByRole('button', { name: '샤라웃 알아보기' }));
  expect(onExplore).toHaveBeenCalledTimes(1);
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  unmount();
  render(<FirstVisitGuide mode="offer" onExplore={onExplore} />);
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

it.each(['바로 둘러볼게요', '소개 안내 닫기'])('%s로 이동 없이 닫을 수 있다', async (name) => {
  const onExplore = jest.fn();
  render(<FirstVisitGuide mode="offer" onExplore={onExplore} />);
  await userEvent.click(screen.getByRole('button', { name }));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(onExplore).not.toHaveBeenCalled();
});

it('Escape로 닫아도 다시 표시하지 않는다', () => {
  render(<FirstVisitGuide mode="offer" onExplore={jest.fn()} />);
  fireEvent(screen.getByRole('dialog'), new Event('cancel', { bubbles: true }));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(localStorage.getItem(key)).toBe('true');
});

it('소개 페이지 직접 방문도 안내를 본 것으로 기록한다', () => {
  const { unmount } = render(<FirstVisitGuide mode="visited" onExplore={jest.fn()} />);
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(localStorage.getItem(key)).toBe('true');
  unmount();
  render(<FirstVisitGuide mode="offer" onExplore={jest.fn()} />);
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

it('안내 제외 모드에서는 표시하거나 방문 기록을 남기지 않는다', () => {
  render(<FirstVisitGuide mode="skip" onExplore={jest.fn()} />);
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(localStorage.getItem(key)).toBeNull();
});

it('로컬 스토리지가 차단되어도 안내를 닫고 페이지로 이동할 수 있다', async () => {
  jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
    throw new Error('blocked');
  });
  jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
    throw new Error('blocked');
  });
  const onExplore = jest.fn();
  render(<FirstVisitGuide mode="offer" onExplore={onExplore} />);
  await userEvent.click(screen.getByRole('button', { name: '샤라웃 알아보기' }));
  expect(onExplore).toHaveBeenCalledTimes(1);
});
