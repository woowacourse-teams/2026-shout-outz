import { render, screen } from '@testing-library/react';
import { createMemoryHistory, createRouter, RouterContextProvider } from '@tanstack/react-router';
import { RelatedQuestions } from '@/components/recommendations/RelatedQuestions';
import { mockRecommendations } from '@/mocks/recommendations';
import { routeTree } from '@/routeTree.gen';

it('같은 상세 화면에서 리렌더해도 선택한 두 질문이 유지된다', () => {
  const router = createRouter({
    routeTree,
    history: createMemoryHistory({ initialEntries: ['/community/3'] }),
  });
  const tree = () => (
    <RouterContextProvider router={router}>
      <RelatedQuestions items={[...mockRecommendations]} />
    </RouterContextProvider>
  );
  const { rerender } = render(tree());
  const titles = screen.getAllByRole('button').map((link) => link.getAttribute('aria-label'));
  expect(titles).toHaveLength(2);
  rerender(tree());
  expect(screen.getAllByRole('button').map((link) => link.getAttribute('aria-label'))).toEqual(
    titles,
  );
});
