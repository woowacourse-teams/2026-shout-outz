import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryHistory, createRouter, RouterContextProvider } from '@tanstack/react-router';
import { RecommendationCard } from '@/components/recommendations/RecommendationCard';
import { mockRecommendations } from '@/mocks/recommendations';
import { routeTree } from '@/routeTree.gen';
import { analytics, type AnalyticsEvent } from '@/utils/analytics';

const recommendation = mockRecommendations[0]!;

/** 화면 진입을 테스트에서 직접 일으키기 위한 가짜 IntersectionObserver. */
let observers: { callback: IntersectionObserverCallback; disconnected: boolean }[] = [];
class FakeIntersectionObserver {
  private entry: { callback: IntersectionObserverCallback; disconnected: boolean };
  constructor(callback: IntersectionObserverCallback) {
    this.entry = { callback, disconnected: false };
    observers.push(this.entry);
  }
  observe() {}
  disconnect() {
    this.entry.disconnected = true;
  }
}

/** 카드가 화면에 보이는 비율을 바꾼다. 0이면 화면 밖이다. */
function intersect(intersectionRatio: number) {
  act(() => {
    for (const observer of observers.filter((item) => !item.disconnected)) {
      observer.callback(
        [{ isIntersecting: intersectionRatio > 0, intersectionRatio } as IntersectionObserverEntry],
        {} as IntersectionObserver,
      );
    }
  });
}

let events: AnalyticsEvent[];
beforeEach(() => {
  observers = [];
  events = [];
  window.IntersectionObserver =
    FakeIntersectionObserver as unknown as typeof window.IntersectionObserver;
  jest.spyOn(analytics, 'track').mockImplementation((event) => void events.push(event));
});
afterEach(() => {
  // @ts-expect-error jsdom에는 원래 IntersectionObserver가 없다.
  delete window.IntersectionObserver;
  jest.restoreAllMocks();
});

function show(position = 1, variant: 'list' | 'detail' = 'list') {
  const router = createRouter({
    routeTree,
    history: createMemoryHistory({ initialEntries: ['/community/3'] }),
  });
  const tree = () => (
    <RouterContextProvider router={router}>
      <RecommendationCard
        recommendation={recommendation}
        surface="feed_detail"
        position={position}
        variant={variant}
      />
    </RouterContextProvider>
  );
  const view = render(tree());
  return { router, rerender: () => view.rerender(tree()) };
}

it('카드가 절반 이상 보이면 노출을 한 번만 보내고, 리렌더나 스크롤 왕복으로 다시 보내지 않는다', () => {
  const { rerender } = show();
  expect(events).toEqual([]);
  intersect(0);
  intersect(0.3);
  expect(events).toEqual([]);

  intersect(0.5);
  intersect(0);
  intersect(1);
  rerender();
  intersect(1);

  expect(events).toEqual([
    {
      name: 'recommendation_impression',
      recommendationId: recommendation.recommendationId,
      surface: 'feed_detail',
      position: 1,
    },
  ]);
});

it('카드를 누르면 화면과 순서를 담아 클릭을 보내고 미리보기로 이동한다', async () => {
  const user = userEvent.setup();
  const { router } = show(0);
  await user.click(
    screen.getByRole('button', {
      name: `${recommendation.title}, AI 추천 질문, 아직 등록되지 않았어요`,
    }),
  );
  expect(events).toContainEqual({
    name: 'recommendation_clicked',
    recommendationId: recommendation.recommendationId,
    surface: 'feed_detail',
    position: 0,
  });
  expect(router.state.location.pathname).toBe('/community/recommendations');
});

it.each(['list', 'detail'] as const)(
  '%s 추천 카드는 제목 버튼 하나와 짧은 본문 설명으로 탐색한다',
  async (variant) => {
    show(0, variant);
    const name = `${recommendation.title}, AI 추천 질문, 아직 등록되지 않았어요`;
    const heading = screen.getByRole('heading', { level: 3, name: recommendation.title });
    const button = within(heading).getByRole('button', { name });
    expect(screen.getAllByRole('button')).toHaveLength(1);
    expect(button).toHaveAccessibleDescription(
      '조회 성능을 높이기 위해 캐시 도입을 고민하고 있어요. DB의 데이터가 변경됐을 때 캐시를 언제, 어떻게 갱신하는지 궁금합니다. TTL만으로 충분한 경우는 언제인가요? 캐시 무효화…',
    );
    const user = userEvent.setup();
    await user.tab();
    expect(button).toHaveFocus();
  },
);
