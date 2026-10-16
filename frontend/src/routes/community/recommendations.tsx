import { createFileRoute, useLocation, useRouter } from '@tanstack/react-router';
import { RecommendationPage } from '@/pages/RecommendationPage';
import { readRecommendationNavigation } from '@/utils/recommendation';

export const Route = createFileRoute('/community/recommendations')({
  component: RecommendationRoute,
});

function RecommendationRoute() {
  const state = useLocation({ select: (location) => location.state });
  const router = useRouter();
  const navigate = Route.useNavigate();
  const context = readRecommendationNavigation(state.questionRecommendation);

  return (
    <RecommendationPage
      recommendation={context?.recommendation}
      onBack={() => {
        if (context && state.__TSR_index > 0) router.history.back();
        else void navigate({ to: '/community', search: { type: 'QUESTION', sort: 'LATEST' } });
      }}
      onCreate={() => {
        if (!context) return;
        void navigate({
          to: '/community/new',
          search: { type: 'QUESTION' },
          state: { questionDraft: context, questionDraftFromPreview: true },
        });
      }}
    />
  );
}
