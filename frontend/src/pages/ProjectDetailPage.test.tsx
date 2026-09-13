/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { act, render, screen, within } from '@testing-library/react';
import { hydrateRoot } from 'react-dom/client';
import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import userEvent from '@testing-library/user-event';
import { dehydrate, QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { renderToString } from 'react-dom/server';
import { handlers } from '@/api/mock/handlers';
import { projectDetailQueryOptions } from '@/api/project-detail';
import { ProjectDetailContent } from '@/components/projects/ProjectDetailContent';
import { MarkdownContent } from '@/components/MarkdownContent';
import { routeTree } from '@/routeTree.gen';

const server = setupServer(...handlers);
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
  window.scrollTo = jest.fn();
});
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

function renderPage(path = '/projects/1') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  const router = createRouter({
    routeTree,
    history: createMemoryHistory({ initialEntries: [path] }),
    scrollRestoration: false,
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
}

test('상세 직접 진입 시 API 정보와 공통 레이아웃을 표시한다', async () => {
  renderPage();
  expect(await screen.findByRole('heading', { level: 1, name: 'Dropit' })).toBeInTheDocument();
  expect(screen.getByText(/정우진 \(작성자\)/)).toBeInTheDocument();
  expect(screen.getByText('React', { selector: 'span' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: '서비스 바로가기 ↗' })).toHaveAttribute(
    'rel',
    'noopener noreferrer',
  );
  expect(
    within(screen.getByRole('banner')).getByRole('link', { name: '프로젝트' }),
  ).toHaveAttribute('aria-current', 'page');
  expect(screen.getByRole('contentinfo')).toBeInTheDocument();
  expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
  expect(await screen.findByText('좋아요 안 함')).toBeInTheDocument();
});

test('목록 카드를 누르면 숫자 ID의 상세 페이지로 이동한다', async () => {
  renderPage('/projects');
  const list = await screen.findByRole('list', { name: '프로젝트 목록' });
  const firstLink = within(list).getAllByRole('link')[0];
  if (!firstLink) throw new Error('프로젝트 상세 링크를 찾을 수 없습니다.');
  expect(firstLink).toHaveAttribute('href', '/projects/1');
  await userEvent.click(firstLink);
  expect(await screen.findByRole('heading', { level: 1, name: 'Dropit' })).toBeInTheDocument();
});

test('미승인 응답은 공개 상세 콘텐츠와 캐시에 사용하지 않는다', async () => {
  const data = await (await fetch('http://localhost/api/v1/projects/1')).json();
  server.use(
    http.get('/api/v1/projects/1', () =>
      HttpResponse.json({
        ...data,
        data: {
          ...data.data,
          approvalStatus: 'REJECTED',
          rejectReason: 'private-reason',
        },
      }),
    ),
  );
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  await expect(queryClient.fetchQuery(projectDetailQueryOptions('1'))).rejects.toThrow(
    '프로젝트가 없거나 접근할 수 없습니다.',
  );
  expect(JSON.stringify(dehydrate(queryClient))).not.toContain('private-reason');
  queryClient.clear();
});

test('빈 멤버·태그·본문을 안내하고 누락된 링크를 숨긴다', async () => {
  const data = await (await fetch('http://localhost/api/v1/projects/1')).json();
  server.use(
    http.get('/api/v1/projects/1', () =>
      HttpResponse.json({
        ...data,
        data: {
          ...data.data,
          members: [],
          techTags: [],
          descriptionMd: '',
          deploymentUrl: null,
          githubRepositoryUrl: null,
        },
      }),
    ),
  );
  renderPage();
  expect(await screen.findByText('등록된 참여 크루가 없습니다.')).toBeInTheDocument();
  expect(screen.getByText('등록된 기술 스택이 없습니다.')).toBeInTheDocument();
  expect(screen.getByText('등록된 프로젝트 소개가 없습니다.')).toBeInTheDocument();
  expect(screen.queryByRole('link', { name: /서비스 바로가기/ })).not.toBeInTheDocument();
  expect(screen.queryByRole('link', { name: /GitHub 저장소/ })).not.toBeInTheDocument();
});

test.each(['/projects/999', '/projects/not-an-id'])(
  '%s는 404 안내와 레이아웃을 표시한다',
  async (path) => {
    const error = jest.spyOn(console, 'error').mockImplementation(() => {});
    try {
      renderPage(path);
      expect(await screen.findByRole('alert')).toHaveTextContent(
        '프로젝트가 없거나 접근할 수 없습니다.',
      );
      expect(screen.getByRole('banner')).toBeInTheDocument();
      expect(screen.getByRole('contentinfo')).toBeInTheDocument();
    } finally {
      error.mockRestore();
    }
  },
);

test('실패 후 다시 시도하면 상세 정보를 표시한다', async () => {
  server.use(
    http.get('/api/v1/projects/1', () => new HttpResponse(null, { status: 500 }), { once: true }),
  );
  const error = jest.spyOn(console, 'error').mockImplementation(() => {});
  try {
    renderPage();
    const retry = await screen.findByRole('button', { name: '다시 시도' });
    expect(screen.getByRole('banner')).toBeInTheDocument();
    await userEvent.click(retry);
    expect(await screen.findByRole('heading', { level: 1, name: 'Dropit' })).toBeInTheDocument();
  } finally {
    error.mockRestore();
  }
});

test('Markdown의 HTML·표는 렌더링하고 스크립트·style·위험한 URL은 제거한다', () => {
  const { container } = render(
    <MarkdownContent>
      {
        '<details><summary>설명 펼치기</summary>내용</details>\n\n| 기술 |\n| --- |\n| React |\n\n<p style="position:fixed" onclick="alert(1)">소개</p><script>alert(1)</script><a href="javascript:alert(1)">링크</a>'
      }
    </MarkdownContent>,
  );
  expect(screen.getByText('설명 펼치기')).toBeInTheDocument();
  expect(screen.getByRole('table')).toBeInTheDocument();
  expect(screen.getByText('소개')).not.toHaveAttribute('style');
  expect(screen.getByText('소개')).not.toHaveAttribute('onclick');
  expect(container.querySelector('script')).toBeNull();
  expect(screen.getByText('링크').getAttribute('href') ?? '').not.toMatch(/javascript:/);
});

test('SSG 결과와 공개 Query 캐시에 개인화 값·반려 사유가 포함되지 않는다', async () => {
  const data = await (await fetch('http://localhost/api/v1/projects/1')).json();
  server.use(
    http.get('/api/v1/projects/1', () =>
      HttpResponse.json({
        ...data,
        data: {
          ...data.data,
          likedByMe: true,
          bookmarkedByMe: true,
          rejectReason: 'private-reason',
        },
      }),
    ),
  );
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: Infinity } },
  });
  await queryClient.ensureQueryData(projectDetailQueryOptions('1'));
  const html = renderToString(
    <QueryClientProvider client={queryClient}>
      <ProjectDetailContent projectId="1" />
    </QueryClientProvider>,
  );
  expect(html).toContain('상태 확인 중');
  expect(html).not.toContain('좋아요 함');
  const serialized = JSON.stringify(dehydrate(queryClient));
  expect(serialized).not.toMatch(/likedByMe|bookmarkedByMe|private-reason/);
  const container = document.createElement('div');
  container.innerHTML = html;
  document.body.append(container);
  const onRecoverableError = jest.fn();
  let root: ReturnType<typeof hydrateRoot> | undefined;
  try {
    await act(async () => {
      root = hydrateRoot(
        container,
        <QueryClientProvider client={queryClient}>
          <ProjectDetailContent projectId="1" />
        </QueryClientProvider>,
        { onRecoverableError },
      );
    });
    expect(await within(container).findByText('좋아요 함')).toBeInTheDocument();
    expect(onRecoverableError).not.toHaveBeenCalled();
  } finally {
    await act(async () => root?.unmount());
    container.remove();
  }
  queryClient.clear();
});
