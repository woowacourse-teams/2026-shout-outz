import { StrictMode, act } from 'react';
import { hydrateRoot } from 'react-dom/client';
import { renderToString } from 'react-dom/server';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AboutPage } from '@/pages/AboutPage';
import snapshot from '@/pages/about/prototype-snapshot.json';

test('SSG includes the first example and hydrates without replacing its content', async () => {
  const container = document.createElement('div');
  container.innerHTML = renderToString(<AboutPage />);
  document.body.append(container);
  expect(
    within(container).getByText(snapshot.topicSamples.concurrency.groups[0]!.title),
  ).toBeInTheDocument();
  expect(container.querySelector('.ref-job-header img')).toHaveAttribute(
    'src',
    '/about-assets/assets/toss-bank.png',
  );
  const heading = within(container).getByRole('heading', { level: 1 });
  const onRecoverableError = jest.fn();
  let root: ReturnType<typeof hydrateRoot>;
  await act(async () => {
    root = hydrateRoot(
      container,
      <StrictMode>
        <AboutPage />
      </StrictMode>,
      { onRecoverableError },
    );
  });
  expect(onRecoverableError).not.toHaveBeenCalled();
  expect(within(container).getByRole('heading', { level: 1 })).toBe(heading);
  await userEvent.click(within(container).getAllByRole('button', { name: 'N+1' })[0]!);
  expect(within(container).getByText('N+1 문제')).toBeInTheDocument();
  await act(async () => root.unmount());
  container.remove();
});

test('StrictMode, independent cards, example selection and remount keep working', async () => {
  const user = userEvent.setup();
  const view = render(
    <StrictMode>
      <AboutPage />
    </StrictMode>,
  );
  await user.click(screen.getAllByRole('button', { name: 'N+1' })[0]!);
  expect(screen.getByRole('searchbox', { name: '경험 검색' })).toHaveValue('N+1');
  await user.click(screen.getByRole('button', { name: snapshot.jobKeywords[1]!.keyword }));
  expect(screen.getByText(snapshot.jobKeywords[1]!.groups[0]!.title)).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: '조회 성능 개선' }));
  expect(screen.getByRole('heading', { name: /반복 조회를 줄인\s+쿼리 개선/ })).toBeInTheDocument();
  const summaries = screen.getAllByText('고른 이유와 한 일 보기');
  await user.click(summaries[0]!);
  await user.click(summaries[1]!);
  expect(summaries[0]!.closest('details')).toHaveAttribute('open');
  expect(summaries[1]!.closest('details')).toHaveAttribute('open');
  view.unmount();
  render(
    <StrictMode>
      <AboutPage />
    </StrictMode>,
  );
  expect(screen.getByRole('searchbox', { name: '경험 검색' })).toHaveValue('동시성');
  await user.click(screen.getByRole('button', { name: '조회 성능 개선' }));
  expect(screen.getByRole('heading', { name: /반복 조회를 줄인\s+쿼리 개선/ })).toBeInTheDocument();
});

test('events stay inside the mounted page and cleanup detaches old listeners', async () => {
  const user = userEvent.setup();
  const first = render(<AboutPage />);
  const second = render(<AboutPage />);
  const detachedButton = within(first.container).getByRole('button', { name: '조회 성능 개선' });
  const detachedPage = first.container.querySelector('.about-page')!;
  await user.click(detachedButton);
  expect(
    within(second.container).queryByRole('heading', { name: /반복 조회를 줄인\s+쿼리 개선/ }),
  ).not.toBeInTheDocument();
  first.unmount();
  detachedPage.querySelector('#jd-result-title')!.textContent = 'unmounted';
  detachedButton.click();
  expect(detachedPage.querySelector('#jd-result-title')).toHaveTextContent('unmounted');
});

test('랜딩 본문의 외부 이동은 프로토타입 링크 하나로 제공한다', () => {
  const { container } = render(<AboutPage />);
  const externalLinks = [...container.querySelectorAll('a[href^="http"]')];
  expect(externalLinks).toHaveLength(1);
  expect(externalLinks[0]).toHaveAttribute('href', 'https://project-after-psi.vercel.app/#/about');
});

test.each([
  ['무중단 배포', 'deployment'],
  ['인덱스', 'index'],
  ['React 렌더링', 'react'],
  ['WebSocket', 'websocket'],
] as const)('추천 검색어 %s를 선택하면 해당 주제의 자료를 보여준다', async (keyword, key) => {
  const user = userEvent.setup();
  render(
    <StrictMode>
      <AboutPage />
    </StrictMode>,
  );
  await user.click(screen.getAllByRole('button', { name: keyword })[0]!);
  expect(screen.getByRole('searchbox', { name: '경험 검색' })).toHaveValue(keyword);
  expect(document.querySelector('#ref-category')).toHaveTextContent(
    snapshot.topicSamples[key].category,
  );
  const first = snapshot.topicSamples[key].groups[0]!;
  expect(screen.getAllByText(first.title).length).toBeGreaterThan(0);
  expect(document.querySelector('#ref-groups a')).toBeNull();
});

test('공고 역량을 선택하면 해당 스냅샷의 자료와 빈 단계를 표시한다', async () => {
  const user = userEvent.setup();
  render(<AboutPage />);
  for (const keyword of snapshot.jobKeywords) {
    expect(screen.getByRole('button', { name: keyword.keyword })).toBeInTheDocument();
  }
  const redis = snapshot.jobKeywords.find((item) => item.keyword === 'Redis 캐시')!;
  await user.click(screen.getByRole('button', { name: redis.keyword }));
  expect(screen.getByText(redis.reason)).toBeInTheDocument();
  expect(screen.getAllByText('이 단계 자료는 아직 없어요.').length).toBeGreaterThan(0);
});

test('프로젝트 예시는 API 요청 없이 정적 HTML과 화면에 표시된다', () => {
  const html = renderToString(<AboutPage />);
  expect(html).toContain('팀의 결정을 모두의 기억으로, Knot');
  expect(html).toContain('자리하나?');
  expect(html).toContain('여기담');
  expect(html).not.toContain('프로젝트를 불러오는 중입니다');
  render(<AboutPage />);
  const projects = screen.getByRole('list', { name: '프로젝트 예시' });
  expect(within(projects).getByRole('heading', { name: 'Knot' })).toBeInTheDocument();
  expect(within(projects).getByRole('heading', { name: '자리하나?' })).toBeInTheDocument();
  expect(within(projects).getByRole('heading', { name: '여기담' })).toBeInTheDocument();
});
