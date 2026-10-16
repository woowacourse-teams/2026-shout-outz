import { createFileRoute, useLocation } from '@tanstack/react-router';

import {
  clearLoginQuestionDraft,
  readLoginQuestionDraft,
  readRecommendationNavigation,
} from '@/utils/recommendation';
import { SignupPage } from '@/pages/SignupPage';

export const Route = createFileRoute('/signup')({ component: SignupRoute });

function SignupRoute() {
  const navigate = Route.useNavigate();

  const state = useLocation({ select: (location) => location.state.questionDraft });
  const draft = readRecommendationNavigation(state) ?? readLoginQuestionDraft();
  return (
    <SignupPage
      completionLabel={draft ? '질문 작성 계속하기' : undefined}
      onComplete={() => {
        if (draft) {
          void navigate({
            to: '/community/new',
            search: { type: 'QUESTION' },
            state: { questionDraft: draft },
            replace: true,
          }).then(clearLoginQuestionDraft);
        } else void navigate({ to: '/' });
      }}
    />
  );
}
