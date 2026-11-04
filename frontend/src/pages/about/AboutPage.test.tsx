import { StrictMode, act, type ComponentProps } from 'react';
import { hydrateRoot } from 'react-dom/client';
import { renderToString } from 'react-dom/server';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { AboutPage } from '@/pages/AboutPage';
import job from '@/pages/about/data/job.json';
import { undot } from '@/pages/about/resources';

// 라우터 없이 화면만 검증한다. 실제 라우트 연결은 AboutRoute.test.tsx에서 다룬다.
jest.mock('@tanstack/react-router', () => ({
  Link: ({
    to,
    params,
    ...props
  }: ComponentProps<'a'> & { to: string; params?: Record<string, string> }) => (
    <a
      href={Object.entries(params ?? {}).reduce(
        (path, [key, value]) => path.replace(`$${key}`, value),
        to,
      )}
      {...props}
    />
  ),
}));

const resultsSection = () => screen.getByRole('region', { name: '우테코 경험 전체 보기' });

test('정적 HTML에 기본 검색 결과가 들어 있고 StrictMode hydration에서 내용을 바꾸지 않는다', async () => {
  const container = document.createElement('div');
  container.innerHTML = renderToString(<AboutPage />);
  document.body.append(container);
  expect(container.textContent).toContain('동시성 제어/락 주제로 찾았어요.');
  expect(container.textContent).toContain('Rilog.');

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
  await act(async () => root.unmount());
  container.remove();
});

test('추천 검색어를 누르면 그 주제의 자료와 검색 화면 링크로 바뀐다', async () => {
  const user = userEvent.setup();
  render(<AboutPage />);
  await user.click(screen.getByRole('button', { name: 'N+1' }));

  expect(screen.getByRole('button', { name: 'N+1' })).toHaveAttribute('aria-pressed', 'true');
  expect(screen.getByRole('button', { name: '동시성' })).toHaveAttribute('aria-pressed', 'false');
  expect(within(resultsSection()).getByText('N+1 문제')).toBeInTheDocument();
  expect(screen.getByRole('searchbox', { name: '기술 이야기 검색' })).toHaveValue('N+1');
  expect(
    within(resultsSection()).getByRole('link', { name: /검색 화면에서 더 찾아보기/ }),
  ).toHaveAttribute('href', 'https://project-after-psi.vercel.app/#/projects/search?q=N%2B1');
});

test('직접 검색하면 담아 두지 않은 주제는 검색 화면으로, 없는 주제는 안내로 보여 준다', async () => {
  const user = userEvent.setup();
  render(<AboutPage />);
  const input = screen.getByRole('searchbox', { name: '기술 이야기 검색' });

  await user.clear(input);
  await user.type(input, 'JPA{Enter}');
  expect(
    within(resultsSection()).getByText("'JPA/Hibernate' 주제 자료는 검색 화면에서 볼 수 있어요."),
  ).toBeInTheDocument();

  await user.clear(input);
  await user.type(input, '없는주제어{Enter}');
  expect(
    within(resultsSection()).getByText("'없는주제어'에 맞는 기술 주제를 아직 못 찾았어요."),
  ).toBeInTheDocument();
});

test('공고 역량을 고르면 그 역량의 이유와 자료로 바뀐다', async () => {
  const user = userEvent.setup();
  render(<AboutPage />);
  const jobs = screen.getByRole('region', { name: '채용 공고에 맞는 프로젝트 사례 찾기' });
  const target = job.keywords[2]!;

  await user.click(within(jobs).getByRole('button', { name: undot(target.keyword) }));
  expect(within(jobs).getByRole('button', { name: undot(target.keyword) })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  expect(within(jobs).getByText(undot(target.reason))).toBeInTheDocument();
  // 가운뎃점은 프로토타입처럼 쉼표로 보여 준다.
  expect(within(jobs).getByRole('button', { name: 'Kotlin, Spring' })).toBeInTheDocument();
});

test('소개 페이지의 프로젝트와 공고를 프로토타입으로 연결한다', () => {
  render(<AboutPage />);
  const projects = screen.getByRole('list', { name: '추천 프로젝트' });
  expect(
    within(projects)
      .getAllByRole('link')
      .map((link) => link.getAttribute('href')),
  ).toEqual(Array(3).fill('https://project-after-psi.vercel.app/#/projects'));
  expect(screen.getByRole('link', { name: '프로젝트 전체 둘러보기' })).toHaveAttribute(
    'href',
    'https://project-after-psi.vercel.app/#/projects',
  );
  expect(screen.getByRole('link', { name: '공고 키워드로 찾기' })).toHaveAttribute(
    'href',
    'https://project-after-psi.vercel.app/#/projects/jobs',
  );
  expect(
    screen.getByRole('link', { name: '샤라웃의 실제 이력서와 포트폴리오 열기' }),
  ).toHaveAttribute('href', 'https://project-after-psi.vercel.app/#/projects/@shout-outz');
});

test('기술 장면의 공고 요구사항을 고르면 연결되는 경험이 바뀐다', async () => {
  const user = userEvent.setup();
  const { container } = render(<AboutPage />);
  expect(container.querySelectorAll('.technology-scope .scene')).toHaveLength(4);

  await user.click(screen.getByRole('button', { name: '조회 성능 개선' }));
  expect(screen.getByRole('button', { name: '조회 성능 개선' })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  expect(screen.getByRole('button', { name: '동시성 제어' })).toHaveAttribute(
    'aria-pressed',
    'false',
  );
  expect(container.querySelector('.jd-visual')).toHaveClass('is-matching');
  expect(container.querySelector('.jd-result h4')).toHaveTextContent('반복 조회를 줄인 쿼리 개선');
});

test('AI·백엔드와 프론트엔드 기술 이야기를 보여 주고 렌더링 카드는 카드마다 따로 펼친다', async () => {
  const user = userEvent.setup();
  render(<AboutPage />);
  expect(screen.getByRole('heading', { name: /우리의 기술 이야기/ })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: /세 가지 고민/ })).toBeInTheDocument();
  expect(screen.getByText('08 / BEHIND THE MATCH')).toBeInTheDocument();
  expect(screen.getByText('09 / UNDER THE HOOD · FRONTEND')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: '역량 추출 API 살펴보기 ↗' })).toHaveAttribute(
    'href',
    'https://github.com/woowacourse-teams/2026-shout-outz/pull/526',
  );
  expect(screen.getByRole('link', { name: '분석 Worker 살펴보기 ↗' })).toHaveAttribute(
    'href',
    'https://github.com/woowacourse-teams/2026-shout-outz/pull/537',
  );
  const rendering = screen.getByRole('region', { name: /가장 빨리 보이는 방법으로/ });
  const summaries = within(rendering).getAllByText('고른 이유와 한 일 보기');
  expect(summaries).toHaveLength(3);

  await user.click(summaries[1]!);
  const details = summaries.map((summary) => summary.closest('details')!);
  expect(details.map((item) => item.open)).toEqual([false, true, false]);
});
