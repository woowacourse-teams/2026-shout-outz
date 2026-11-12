/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { renderRoute } from '@/test/renderRoute';
import { analytics } from '@/utils/analytics';

// 첫 방문 안내 모달이 화면을 가리지 않게 이미 본 것으로 둔다.
beforeEach(() => localStorage.setItem('shoutoutz:about-guide-seen', 'true'));
afterEach(() => jest.restoreAllMocks());

const trackedNames = (track: jest.SpyInstance) =>
  track.mock.calls.map(([event]) => (event as { name: string }).name);

describe('탐색 홈', () => {
  test('탭을 바꾸면 바꾼 탭과 이전 탭을 남긴다', async () => {
    const user = userEvent.setup();
    const track = jest.spyOn(analytics, 'track');
    renderRoute('/');

    await user.click(await screen.findByRole('tab', { name: '채용공고' }));

    expect(track).toHaveBeenCalledWith({
      name: 'explore_tab_changed',
      tab: 'job',
      from: 'keyword',
    });
  });

  test('검색창으로 찾으면 검색어 길이와 search_box를 남긴다', async () => {
    const user = userEvent.setup();
    const track = jest.spyOn(analytics, 'track');
    renderRoute('/');

    const input = await screen.findByLabelText('기술 이야기 검색');
    await user.type(input, '동시성{Enter}');

    expect(track).toHaveBeenCalledWith({
      name: 'explore_search_performed',
      tab: 'keyword',
      queryLength: 3,
      source: 'search_box',
    });
  });

  test('추천 검색어를 누르면 suggestion으로 남긴다', async () => {
    const user = userEvent.setup();
    const track = jest.spyOn(analytics, 'track');
    renderRoute('/');

    const suggestions = await screen.findByRole('list', { name: '추천 검색어' });
    const [first] = within(suggestions).getAllByRole('button');
    if (!first) throw new Error('추천 검색어가 없습니다.');
    await user.click(first);

    expect(track).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'explore_search_performed', source: 'suggestion' }),
    );
  });
});

test('프로젝트 상세에서 탭을 바꾸면 slug와 탭을 남긴다', async () => {
  const user = userEvent.setup();
  const track = jest.spyOn(analytics, 'track');
  renderRoute('/projects/@dropit');

  await screen.findByRole('heading', { level: 1, name: 'Dropit' });
  await user.click(screen.getByRole('tab', { name: '소개' }));

  await waitFor(() =>
    expect(track).toHaveBeenCalledWith({
      name: 'project_detail_tab_changed',
      slug: 'dropit',
      tab: 'intro',
    }),
  );
});

test('버그 제보 버튼을 누르면 bug_report_opened를 남긴다', async () => {
  const user = userEvent.setup();
  const track = jest.spyOn(analytics, 'track');
  renderRoute('/');

  await user.click(await screen.findByRole('button', { name: '버그 제보하기' }));

  expect(trackedNames(track)).toContain('bug_report_opened');
});
