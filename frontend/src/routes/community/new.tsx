import { createFileRoute, useLocation, useRouter } from '@tanstack/react-router';
import { FeedEditorPage } from '@/pages/FeedEditorPage';
import { clearLoginQuestionDraft, readRecommendationNavigation } from '@/utils/recommendation';
import type { FeedType } from '@/apis/feed';

export const Route = createFileRoute('/community/new')({
  validateSearch: (search: Record<string, unknown>): { type: FeedType } => ({
    type: search.type === 'QUESTION' ? 'QUESTION' : 'POST',
  }),
  component: FeedCreateRoute,
});

function FeedCreateRoute() {
  const { type } = Route.useSearch();
  const navigate = Route.useNavigate();
  const router = useRouter();
  const state = useLocation({ select: (location) => location.state });
  const draft = type === 'QUESTION' ? readRecommendationNavigation(state.questionDraft) : undefined;
  return (
    <FeedEditorPage
      feedType={type}
      initialDraft={draft}
      onCancel={() => {
        clearLoginQuestionDraft();
        if (draft && state.questionDraftFromPreview && state.__TSR_index > 0) {
          router.history.back();
        } else if (draft)
          void navigate({
            to: '/community/recommendations',
            state: { questionRecommendation: draft },
            replace: true,
          });
        else void navigate({ to: '/community', search: { type, sort: 'LATEST' } });
      }}
      onSaved={(feedId) => {
        clearLoginQuestionDraft();
        void navigate({
          to: '/community/$feedId',
          params: { feedId: String(feedId) },
          replace: true,
        });
      }}
    />
  );
}
