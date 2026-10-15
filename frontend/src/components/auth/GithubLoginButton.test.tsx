import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';

import { GithubLoginButton } from '@/components/auth/GithubLoginButton';
import { analytics } from '@/utils/analytics';

// jsdom은 문서 이동을 지원하지 않으므로 assign이 지원하는 hash 이동으로 검증합니다.
jest.mock('@/utils/auth', () => ({ getGithubLoginUrl: () => '#github-login' }));
jest.mock('@/utils/analytics', () => ({
  analytics: { track: jest.fn() },
  toPathPattern: (path: string) => path,
}));

let frames: Map<number, FrameRequestCallback>;

beforeEach(() => {
  frames = new Map();
  let id = 0;
  jest.spyOn(window, 'requestAnimationFrame').mockImplementation((callback) => {
    frames.set(++id, callback);
    return id;
  });
  jest.spyOn(window, 'cancelAnimationFrame').mockImplementation((id) => {
    frames.delete(id);
  });
  window.history.replaceState(null, '', '/');
  jest.mocked(analytics.track).mockClear();
});

afterEach(() => {
  cleanup();
  jest.restoreAllMocks();
});

function nextFrame() {
  act(() => {
    const callbacks = [...frames.values()];
    frames.clear();
    callbacks.forEach((callback) => callback(0));
  });
}

it('연속 클릭을 막고 오버레이를 표시한 다음 로그인 주소로 이동한다', () => {
  render(<GithubLoginButton>GitHub 로그인</GithubLoginButton>);
  const button = screen.getByRole('button', { name: 'GitHub 로그인' });

  fireEvent.click(button);
  fireEvent.click(button);

  expect(button).toBeDisabled();
  expect(screen.getByRole('dialog', { name: 'GitHub 로그인으로 이동 중' })).toBeVisible();
  expect(screen.getByRole('status')).toHaveTextContent('GitHub 로그인으로 이동 중…');
  expect(analytics.track).toHaveBeenCalledTimes(1);
  expect(document.body.style.overflow).toBe('hidden');
  expect(window.location.hash).toBe('');
  nextFrame();
  expect(window.location.hash).toBe('');
  nextFrame();
  expect(window.location.hash).toBe('#github-login');
});

it('Escape와 배경 클릭으로 진행 화면을 닫지 않는다', () => {
  render(<GithubLoginButton>GitHub 로그인</GithubLoginButton>);
  fireEvent.click(screen.getByRole('button', { name: 'GitHub 로그인' }));
  const dialog = screen.getByRole('dialog');
  const cancel = new Event('cancel', { cancelable: true });

  fireEvent(dialog, cancel);
  const escape = new KeyboardEvent('keydown', {
    key: 'Escape',
    bubbles: true,
    cancelable: true,
  });
  fireEvent(dialog, escape);
  fireEvent.click(dialog);

  expect(cancel.defaultPrevented).toBe(true);
  expect(escape.defaultPrevented).toBe(true);
  expect(dialog).toBeVisible();
});

it('뒤로 가기로 복원되면 오버레이와 잠금을 해제하고 다시 로그인할 수 있다', () => {
  render(<GithubLoginButton>GitHub 로그인</GithubLoginButton>);
  const button = screen.getByRole('button', { name: 'GitHub 로그인' });
  fireEvent.click(button);
  nextFrame();
  nextFrame();

  fireEvent(window, new PageTransitionEvent('pageshow', { persisted: true }));

  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(button).toBeEnabled();
  expect(document.body.style.overflow).toBe('');
  fireEvent.click(button);
  expect(screen.getByRole('dialog')).toBeVisible();
  expect(analytics.track).toHaveBeenCalledTimes(2);
});

it('화면이 사라지면 예약된 로그인 이동을 취소한다', () => {
  const { unmount } = render(<GithubLoginButton>GitHub 로그인</GithubLoginButton>);
  fireEvent.click(screen.getByRole('button', { name: 'GitHub 로그인' }));
  nextFrame();
  unmount();
  nextFrame();
  expect(window.location.hash).toBe('');
  expect(document.body.style.overflow).toBe('');
});

it('Tab과 Shift+Tab으로 로딩 화면 밖에 포커스가 이동하지 않는다', () => {
  render(<GithubLoginButton>GitHub 로그인</GithubLoginButton>);
  fireEvent.click(screen.getByRole('button', { name: 'GitHub 로그인' }));
  const dialog = screen.getByRole('dialog');
  expect(dialog).toHaveFocus();

  for (const shiftKey of [false, true]) {
    const event = new KeyboardEvent('keydown', {
      key: 'Tab',
      shiftKey,
      bubbles: true,
      cancelable: true,
    });
    fireEvent(dialog, event);
    expect(event.defaultPrevented).toBe(true);
  }
});
