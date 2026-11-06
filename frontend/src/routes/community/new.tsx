import { useState } from 'react';
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

function isCreatePath(pathname: string) {
  return pathname.replace(/\/+$/, '') === '/community/new';
}

function FeedCreateRoute() {
  const { type } = Route.useSearch();
  const navigate = Route.useNavigate();
  const router = useRouter();
  // 다른 주소로 이동을 시작하면 이 화면이 아직 그려진 채로 location이 다음 주소로 먼저 바뀐다.
  // 그 state에는 questionDraft가 없어 FeedForm의 key가 바뀌고 다시 마운트되므로,
  // 이 주소에 있을 때 받은 state만 사용한다.
  const current = useLocation({
    select: (location) => (isCreatePath(location.pathname) ? location.state : undefined),
  });
  const [state, setState] = useState(current);
  if (current && current !== state) setState(current);
  const draft =
    type === 'QUESTION' ? readRecommendationNavigation(state?.questionDraft) : undefined;
  // 미리보기의 작성 버튼만 questionDraftFromPreview를 붙인다. 그 표시 없이 초안을 들고 오는 길은
  // 로그인(가입) 뒤 복귀(ResumeQuestionDraft, 가입 완료)뿐이다.
  const entry = !draft
    ? 'button'
    : state?.questionDraftFromPreview
      ? 'recommendation'
      : 'login_resume';
  return (
    <FeedEditorPage
      feedType={type}
      initialDraft={draft}
      entry={entry}
      onCancel={() => {
        clearLoginQuestionDraft();
        if (draft && state?.questionDraftFromPreview && state.__TSR_index > 0) {
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
