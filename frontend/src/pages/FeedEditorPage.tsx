import { Title } from '@/components/Title';
import { useState } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import { feedQuery, type FeedType } from '@/apis/feed';
import { sessionQuery } from '@/apis/session';
import { Footer } from '@/components/Footer';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { FeedForm } from '@/components/feeds/FeedForm';
import type { RecommendationNavigation } from '@/types/recommendation';
import { saveLoginQuestionDraft } from '@/utils/recommendation';
import { LoginRequiredGuide } from '@/components/auth/LoginRequiredGuide';

interface FeedEditorPageProps {
  feedId?: number;
  feedType?: FeedType;
  initialDraft?: RecommendationNavigation;
  onCancel: () => void;
  onSaved: (feedId: number) => void;
}

export function FeedEditorPage(props: FeedEditorPageProps) {
  const label = props.feedType === 'QUESTION' ? '질문' : '피드';
  const title = props.feedId === undefined ? `${label} 작성` : '글 수정';
  return (
    <div className="bg-background flex min-h-dvh flex-col">
      <Title>{`${title} | shout-outz`}</Title>
      <AppGnb />
      <main className="flex-1">
        <AsyncBoundary>
          <FeedEditorContent {...props} title={title} />
        </AsyncBoundary>
      </main>
      <Footer />
    </div>
  );
}

function FeedEditorContent({ title, ...props }: FeedEditorPageProps & { title: string }) {
  const { data: session } = useSuspenseQuery(sessionQuery);
  const [draftError, setDraftError] = useState(false);

  if (session.status !== 'AUTHENTICATED' || session.userId == null) {
    return (
      <LoginRequiredGuide
        pageName={title}
        fallbackTo="/community"
        onBeforeLogin={() => {
          if (!props.initialDraft) return true;
          const saved = saveLoginQuestionDraft(props.initialDraft);
          setDraftError(!saved);
          return saved;
        }}
      >
        {props.initialDraft && (
          <p className="mt-6 text-sm text-gray-500">
            로그인 후 선택한 추천 질문으로 이어서 작성할 수 있어요.
          </p>
        )}
        {draftError && (
          <p role="alert" className="mt-3 text-sm text-red-600">
            초안을 보관하지 못했어요. 브라우저의 사이트 저장소를 허용한 뒤 다시 시도해 주세요.
          </p>
        )}
      </LoginRequiredGuide>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-8 px-6 pt-12 pb-24">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-extrabold tracking-tight text-gray-900">{title}</h1>
        <p className="text-base text-gray-600">
          {props.feedId !== undefined
            ? '작성한 내용을 수정해보세요.'
            : props.feedType === 'QUESTION'
              ? '크루에게 묻고 싶은 내용을 자유롭게 작성해 보세요.'
              : '나누고 싶은 기술 아티클, 트러블슈팅 경험, 프로젝트 회고를 자유롭게 공유해보세요.'}
        </p>
      </div>
      <AsyncBoundary>
        <EditorForm {...props} userId={session.userId} />
      </AsyncBoundary>
    </div>
  );
}

function EditorForm({ userId, ...props }: FeedEditorPageProps & { userId: number }) {
  return props.feedId === undefined ? (
    <FeedForm
      key={`${userId}-${props.initialDraft?.recommendation.recommendationId ?? 'new'}`}
      userId={userId}
      feedType={props.feedType ?? 'POST'}
      initialDraft={props.initialDraft?.recommendation}
      onCancel={props.onCancel}
      onSaved={props.onSaved}
    />
  ) : (
    <EditForm
      key={`${userId}-${props.feedId}`}
      userId={userId}
      feedId={props.feedId}
      onCancel={props.onCancel}
      onSaved={props.onSaved}
    />
  );
}

function EditForm({
  feedId,
  userId,
  onCancel,
  onSaved,
}: Omit<Required<FeedEditorPageProps>, 'feedType' | 'initialDraft'> & { userId: number }) {
  const { data: feed } = useSuspenseQuery(feedQuery(feedId));
  return (
    <FeedForm
      key={feedId}
      userId={userId}
      initialFeed={feed}
      feedType={feed.feedType === 'QUESTION' ? 'QUESTION' : 'POST'}
      onCancel={onCancel}
      onSaved={onSaved}
    />
  );
}
