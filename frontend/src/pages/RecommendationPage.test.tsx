/**
 * @jest-environment ./jest.network-environment.js
 * @jest-environment-options {"customExportConditions":["node","node-addons"]}
 */
import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { renderRoute, server } from '@/test/renderRoute';
import { createFeedHandlers, mockFeeds } from '@/mocks/handlers';
import { mockRecommendations } from '@/mocks/recommendations';
import { readLoginQuestionDraft, saveLoginQuestionDraft } from '@/utils/recommendation';

const recommendation = mockRecommendations[0]!;
const context = { recommendation, from: '/community?type=QUESTION&sort=POPULAR' };

beforeEach(() => {
  server.use(...createFeedHandlers());
  sessionStorage.clear();
});
afterEach(() => {
  sessionStorage.clear();
  jest.restoreAllMocks();
});

function listWithSlot() {
  const feeds = Array.from({ length: 5 }, (_, i) => ({
    ...mockFeeds[2]!,
    feedId: 31 + i,
    title: `기존 질문 ${i + 1}`,
  }));
  server.use(
    http.get('*/api/v1/feeds', () =>
      HttpResponse.json({
        status: 'success',
        data: feeds,
        meta: {
          nextCursor: null,
          hasNext: false,
          totalCount: 5,
          recommendationSlots: [{ afterFeedId: 35, recommendation }],
        },
      }),
    ),
  );
}

it('목록의 지정 위치에 추천을 넣고, 미리보기에서 작성 폼까지 내용을 전달해 새 질문을 등록한다', async () => {
  listWithSlot();
  const user = userEvent.setup();
  let submitted: Record<string, unknown> | undefined;
  server.use(
    http.post('*/api/v1/feeds', async ({ request }) => {
      submitted = (await request.json()) as Record<string, unknown>;
      return HttpResponse.json(
        { status: 'success', data: { ...mockFeeds[2], feedId: 103 } },
        { status: 201 },
      );
    }),
    http.get('*/api/v1/feeds/103', () =>
      HttpResponse.json({
        status: 'success',
        data: {
          ...mockFeeds[2],
          feedId: 103,
          title: recommendation.title,
          content: recommendation.content,
        },
      }),
    ),
  );
  const router = renderRoute('/community?type=QUESTION&sort=POPULAR');
  const link = await screen.findByRole('button', { name: `추천 질문: ${recommendation.title}` });
  const fifth = screen.getByRole('link', { name: '기존 질문 5' }).closest('article')!;
  expect(fifth.nextElementSibling).toContainElement(link);
  await user.click(link);
  expect(await screen.findByRole('heading', { level: 1 })).toHaveTextContent(recommendation.title);
  expect(screen.getByLabelText('미등록 추천 질문 안내')).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: '공유' })).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: '저도 궁금해요' })).not.toBeInTheDocument();
  expect(submitted).toBeUndefined();
  expect(router.state.location.href).toBe('/community/recommendations');
  expect(sessionStorage.length).toBe(0);
  await user.click(screen.getAllByRole('button', { name: '이 내용으로 질문 작성하기' })[0]!);
  expect(await screen.findByRole('textbox', { name: '제목' })).toHaveValue(recommendation.title);
  expect(screen.getByRole('textbox', { name: '피드 내용' })).toHaveValue(recommendation.content);
  expect(submitted).toBeUndefined();
  await user.click(screen.getByRole('combobox', { name: '카테고리' }));
  await user.click(screen.getByRole('option', { name: '진로 고민' }));
  await user.click(screen.getByRole('button', { name: '질문 등록하기' }));
  await waitFor(() => expect(router.state.location.pathname).toBe('/community/103'));
  expect(submitted).toEqual(
    expect.objectContaining({
      title: recommendation.title,
      content: recommendation.content,
      feedType: 'QUESTION',
    }),
  );
  expect(submitted).not.toHaveProperty('recommendationId');
});

it('직접 접속하거나 유효한 추천 상태가 없으면 선택 안내를 표시한다', async () => {
  const router = renderRoute('/community/recommendations');
  expect(
    await screen.findByRole('heading', { name: '추천 목록에서 질문을 선택해 주세요' }),
  ).toBeInTheDocument();
  await act(() =>
    router.navigate({
      to: '/community/recommendations',
      state: {},
    }),
  );
  expect(
    screen.getByRole('heading', { name: '추천 목록에서 질문을 선택해 주세요' }),
  ).toBeInTheDocument();
});

it('작성 취소와 미리보기 뒤로 가기로 원래 정렬의 목록에 돌아온다', async () => {
  listWithSlot();
  const user = userEvent.setup();
  const router = renderRoute('/community?type=QUESTION&sort=POPULAR');
  await user.click(
    await screen.findByRole('button', { name: `추천 질문: ${recommendation.title}` }),
  );
  await user.click(
    (await screen.findAllByRole('button', { name: '이 내용으로 질문 작성하기' }))[0]!,
  );
  await user.click(await screen.findByRole('button', { name: '취소' }));
  await user.click((await screen.findAllByRole('button', { name: '돌아가기' }))[1]!);
  await waitFor(() => expect(router.state.location.pathname).toBe('/community'));
  expect(router.state.location.search.sort).toBe('POPULAR');
});

it('비로그인 사용자도 추천을 읽고, 로그인 버튼을 누르는 시점에만 초안을 보관한다', async () => {
  server.use(
    http.get('*/api/v1/auth/session', () =>
      HttpResponse.json({
        status: 'success',
        data: { status: 'UNAUTHENTICATED', userId: null, role: null, csrfToken: null },
      }),
    ),
  );
  jest.spyOn(window, 'requestAnimationFrame').mockReturnValue(1);
  const user = userEvent.setup();
  const router = renderRoute('/community/recommendations');
  await act(() =>
    router.navigate({
      to: '/community/recommendations',
      state: { questionRecommendation: context },
    }),
  );
  await user.click(
    (await screen.findAllByRole('button', { name: '이 내용으로 질문 작성하기' }))[0]!,
  );
  expect(readLoginQuestionDraft()).toBeUndefined();
  await user.click(await screen.findByRole('button', { name: 'GitHub 로그인' }));
  expect(readLoginQuestionDraft()).toEqual(context);
});

it('로그인 완료 후 보관한 초안으로 돌아가고 사용자가 수정한 내용을 다시 덮어쓰지 않는다', async () => {
  saveLoginQuestionDraft(context);
  const user = userEvent.setup();
  const router = renderRoute('/');
  const input = await screen.findByRole('textbox', { name: '제목' });
  expect(router.state.location.pathname).toBe('/community/new');
  expect(input).toHaveValue(recommendation.title);
  await waitFor(() => expect(readLoginQuestionDraft()).toBeUndefined());
  await user.clear(input);
  await user.type(input, '내가 수정한 질문');
  expect(input).toHaveValue('내가 수정한 질문');
});

it('질문 상세에는 추천이 준비됐을 때만 최대 두 개가 본문 다음에 표시된다', async () => {
  server.use(...createFeedHandlers({ recommendations: true }));
  renderRoute('/community/3');
  const section = await screen.findByRole('region', { name: '연관 추천 질문' });
  expect(within(section).getAllByRole('button')).toHaveLength(2);
  const body = screen.getByText(mockFeeds[2]!.content);
  expect(body.compareDocumentPosition(section) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
});

it('가입 완료 화면에서는 초안을 유지하고 완료 버튼으로 작성을 이어간다', async () => {
  saveLoginQuestionDraft(context);
  const user = userEvent.setup();
  const router = renderRoute('/signup');
  const resume = await screen.findByRole('button', { name: '질문 작성 계속하기' });
  expect(router.state.location.pathname).toBe('/signup');
  expect(readLoginQuestionDraft()).toEqual(context);
  await user.click(resume);
  expect(await screen.findByRole('textbox', { name: '제목' })).toHaveValue(recommendation.title);
  await waitFor(() => expect(readLoginQuestionDraft()).toBeUndefined());
});

it.each(['PENDING', 'PROCESSING', 'FAILED'])('%s인 추천은 상세에서 숨긴다', async (status) => {
  server.use(
    http.get('*/api/v1/feeds/3', () =>
      HttpResponse.json({
        status: 'success',
        data: {
          ...mockFeeds[2],
          relatedQuestionRecommendations: { status, items: mockRecommendations },
        },
      }),
    ),
  );
  renderRoute('/community/3');
  await screen.findByText(mockFeeds[2]!.content);
  expect(screen.queryByRole('region', { name: '연관 추천 질문' })).not.toBeInTheDocument();
});
