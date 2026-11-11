/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { render, screen, waitFor, within } from '@testing-library/react';
import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { OverlayProvider } from 'overlay-kit';
import { handlers } from '@/api/mock/handlers';
import { ModalProvider } from '@/components/ModalProvider';
import { MarkdownContent } from '@/components/MarkdownContent';
import { routeTree } from '@/routeTree.gen';
import { PATH_PARAMS_ALLOWED_CHARACTERS } from '@/constants/router';

const server = setupServer(...handlers);
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
  window.scrollTo = jest.fn();
});
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

function renderPage(path = '/projects/@dropit') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  const router = createRouter({
    routeTree,
    pathParamsAllowedCharacters: [...PATH_PARAMS_ALLOWED_CHARACTERS],
    history: createMemoryHistory({ initialEntries: [path] }),
    scrollRestoration: false,
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <ModalProvider>
        <OverlayProvider>
          <RouterProvider router={router} />
        </OverlayProvider>
      </ModalProvider>
    </QueryClientProvider>,
  );
}

test('상세 직접 진입 시 API 정보와 공통 레이아웃을 표시한다', async () => {
  renderPage('/projects/@dropit?tab=intro');
  expect(await screen.findByRole('heading', { level: 1, name: 'Dropit' })).toBeInTheDocument();
  const member = screen.getByRole('link', { name: '정우진 프로필 보기' });
  expect(within(member).getByText('정우진')).toBeInTheDocument();
  expect(within(member).getByText('8기 백엔드 크루')).toBeInTheDocument();
  expect(screen.getByText('React', { selector: 'span' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: '프로젝트 수정' })).toHaveAttribute(
    'href',
    '/projects/@dropit/edit',
  );
  expect(screen.getByRole('link', { name: '서비스 바로가기 ↗' })).toHaveAttribute(
    'rel',
    'noopener noreferrer',
  );
  expect(
    within(screen.getByRole('banner')).getByRole('link', { name: '프로젝트' }),
  ).toHaveAttribute('aria-current', 'page');
  expect(screen.getByRole('contentinfo')).toBeInTheDocument();
  expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
  expect(screen.queryByLabelText('프로젝트 반응')).not.toBeInTheDocument();
});

test('목록 카드를 누르면 slug 주소의 상세 페이지로 이동한다', async () => {
  renderPage('/projects');
  const list = await screen.findByRole('list', { name: '프로젝트 목록' });
  const firstLink = within(list).getAllByRole('link')[0];
  if (!firstLink) throw new Error('프로젝트 상세 링크를 찾을 수 없습니다.');
  expect(firstLink).toHaveAttribute('href', '/projects/@dropit');
  await userEvent.click(firstLink);
  expect(await screen.findByRole('heading', { level: 1, name: 'Dropit' })).toBeInTheDocument();
});

test('프로젝트 좋아요가 slug API를 호출하고 상세 화면에 반영된다', async () => {
  const original = await (await fetch('http://localhost/api/v1/projects/@dropit')).json();
  let liked = false;
  let requested = false;
  server.use(
    http.get('/api/v1/projects/@dropit', () =>
      HttpResponse.json({
        ...original,
        data: { ...original.data, likeCount: liked ? 85 : 84, likedByMe: liked },
      }),
    ),
    http.put('/api/v1/projects/@dropit/reactions/LIKE', () => {
      requested = true;
      liked = true;
      return HttpResponse.json({
        status: 'success',
        data: { slug: 'dropit', type: 'LIKE', active: true, likeCount: 85, bookmarkCount: 28 },
      });
    }),
  );

  renderPage();
  await userEvent.click(await screen.findByRole('button', { name: '프로젝트 좋아요' }));

  await waitFor(() => expect(requested).toBe(true));
  expect(await screen.findByRole('button', { name: '프로젝트 좋아요 취소' })).toHaveTextContent(
    '85',
  );
});

test('승인되지 않은 프로젝트는 공개하지 않는다', async () => {
  const data = await (await fetch('http://localhost/api/v1/projects/@dropit')).json();
  server.use(
    http.get('/api/v1/projects/@dropit', () =>
      HttpResponse.json({
        ...data,
        data: {
          ...data.data,
          approvalStatus: 'REJECTED',
        },
      }),
    ),
  );
  const error = jest.spyOn(console, 'error').mockImplementation(() => {});
  try {
    renderPage();
    expect(await screen.findByRole('alert')).toHaveTextContent(
      '프로젝트가 없거나 접근할 수 없습니다.',
    );
  } finally {
    error.mockRestore();
  }
});

test('빈 멤버·태그·본문을 안내하고 누락된 링크를 숨긴다', async () => {
  const data = await (await fetch('http://localhost/api/v1/projects/@dropit')).json();
  server.use(
    http.get('/api/v1/projects/@dropit', () =>
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
  renderPage('/projects/@dropit?tab=intro');
  expect(await screen.findByText('등록된 참여 크루가 없습니다.')).toBeInTheDocument();
  expect(screen.getByText('등록된 기술 스택이 없습니다.')).toBeInTheDocument();
  expect(screen.getByText('등록된 프로젝트 소개가 없습니다.')).toBeInTheDocument();
  expect(screen.queryByRole('link', { name: /서비스 바로가기/ })).not.toBeInTheDocument();
  expect(screen.queryByRole('link', { name: /GitHub 저장소/ })).not.toBeInTheDocument();
});

test.each(['/projects/@없는-프로젝트', '/projects/@1', '/projects/not-an-id'])(
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
    http.get('/api/v1/projects/@dropit', () => new HttpResponse(null, { status: 500 }), {
      once: true,
    }),
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

describe('프로젝트 상세 탭', () => {
  test('처음에는 경험 탭에서 직군별 경험을 보여주고, 출처를 펼치면 원문 링크가 나온다', async () => {
    renderPage('/projects/@shout-outz');

    const tabs = await screen.findByRole('tablist', { name: '프로젝트 정보' });
    expect(within(tabs).getByRole('tab', { name: '경험' })).toHaveAttribute(
      'aria-selected',
      'true',
    );

    const experiences = await screen.findByRole('list', { name: '경험' });
    expect(
      within(experiences).getByRole('heading', { name: '저사양 EC2의 JVM 메모리 반복 종료 대응' }),
    ).toBeInTheDocument();

    const [toggle] = within(experiences).getAllByRole('button', { name: /출처 \d+개/ });
    if (!toggle) throw new Error('출처 버튼을 찾을 수 없습니다.');
    // 첫 경험의 출처는 처음부터 펼쳐 둔다.
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(
      screen.getByRole('link', { name: /힙 메모리 한도 조정으로 백엔드 강제 종료 방지/ }),
    ).toHaveAttribute('target', '_blank');
    await userEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'false');

    await userEvent.click(
      within(experiences).getByRole('button', {
        name: '저사양 EC2의 JVM 메모리 반복 종료 대응 자세히 보기',
      }),
    );
    const modal = await screen.findByRole('dialog', {
      name: '저사양 EC2의 JVM 메모리 반복 종료 대응',
    });
    expect(within(modal).getByRole('heading', { name: '한 일' })).toBeVisible();
    expect(
      within(modal).getByRole('link', { name: /힙 메모리 한도 조정으로 백엔드 강제 종료 방지/ }),
    ).toHaveAttribute('target', '_blank');
    // jsdom에서는 모바일 바텀시트(vaul)가 열려 포인터 캡처를 못 하므로 Esc로 닫는다.
    await userEvent.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

    const roles = screen.getByRole('tablist', { name: '직군' });
    await userEvent.click(within(roles).getByRole('tab', { name: '프론트엔드' }));
    expect(
      within(screen.getByRole('list', { name: '경험' })).getByRole('heading', {
        name: '모달을 overlay-kit으로 통일',
      }),
    ).toBeInTheDocument();
  });

  test('경험이 없으면 안내를 보여준다', async () => {
    renderPage('/projects/@dropit');

    expect(await screen.findByText('아직 정리된 경험이 없어요.')).toBeInTheDocument();
  });

  test('자료 탭에서 주제를 고르면 그 주제의 팀 자료와 다른 팀 자료를 보여준다', async () => {
    renderPage('/projects/@shout-outz');

    const tabs = await screen.findByRole('tablist', { name: '프로젝트 정보' });
    await userEvent.click(within(tabs).getByRole('tab', { name: '자료' }));

    expect(
      await screen.findByRole('heading', { level: 2, name: '인덱스/쿼리 최적화' }),
    ).toBeInTheDocument();
    expect(await screen.findByRole('region', { name: /^Shout-outz의 이야기/ })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: /^다른 팀의 이야기/ })).toBeInTheDocument();

    const nav = screen.getByRole('navigation', { name: '주제' });
    const [desktopTopic] = within(nav)
      .getAllByRole('button', { name: /무중단 배포/ })
      .filter((button) => button.hasAttribute('aria-current'));
    if (!desktopTopic) throw new Error('주제 버튼을 찾을 수 없습니다.');
    await userEvent.click(desktopTopic);
    expect(
      await screen.findByRole('heading', { level: 2, name: '무중단 배포' }),
    ).toBeInTheDocument();
    expect(desktopTopic).toHaveAttribute('aria-current', 'true');

    const roles = screen.getByRole('tablist', { name: '직군' });
    await userEvent.click(within(roles).getByRole('tab', { name: /프론트엔드/ }));
    // 공통 주제는 프론트엔드에도 있어 고른 주제를 그대로 두고, 백엔드 주제는 목록에서 빠진다.
    await waitFor(() =>
      expect(within(nav).queryAllByRole('button', { name: /인덱스\/쿼리 최적화/ })).toHaveLength(0),
    );
    expect(within(nav).getAllByRole('button', { name: /React 렌더링\/훅/ }).length).toBeGreaterThan(
      0,
    );
    expect(screen.getByRole('heading', { level: 2, name: '무중단 배포' })).toBeInTheDocument();
  });
});
