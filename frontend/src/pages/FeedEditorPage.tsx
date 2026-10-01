import { useSuspenseQuery } from '@tanstack/react-query';
import { feedQuery, type FeedType } from '@/apis/feed';
import { sessionQuery } from '@/apis/session';
import { getButtonStyles } from '@/components/Button';
import { Footer } from '@/components/Footer';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { FeedForm } from '@/components/feeds/FeedForm';
import { getGithubLoginUrl } from '@/utils/auth';

interface FeedEditorPageProps {
  feedId?: number;
  feedType?: FeedType;
  onCancel: () => void;
  onSaved: (feedId: number) => void;
}

export function FeedEditorPage(props: FeedEditorPageProps) {
  const label = props.feedType === 'QUESTION' ? '질문' : '피드';
  const title = props.feedId === undefined ? `${label} 작성` : '글 수정';
  return (
    <div className="bg-background flex min-h-dvh flex-col">
      <title>{`${title} | shout-outz`}</title>
      <AppGnb />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 pt-5 pb-7 md:pt-10 md:pb-20">
        <div className="mx-auto flex w-full max-w-3xl flex-col gap-4 md:gap-7">
          <div className="space-y-2">
            <h1 className="text-2xl font-bold text-gray-900">{title}</h1>
            <p className="hidden text-sm text-gray-600 md:block">
              {props.feedId !== undefined
                ? '작성한 내용을 수정해보세요.'
                : props.feedType === 'QUESTION'
                  ? '크루에게 묻고 싶은 내용을 자유롭게 작성해보세요.'
                  : '나누고 싶은 기술 아티클, 트러블슈팅 경험, 프로젝트 회고를 자유롭게 공유해보세요.'}
            </p>
          </div>
          <AsyncBoundary>
            <AuthenticatedForm {...props} />
          </AsyncBoundary>
        </div>
      </main>
      <Footer />
    </div>
  );
}

function AuthenticatedForm(props: FeedEditorPageProps) {
  const { data: session } = useSuspenseQuery(sessionQuery);
  if (session.status !== 'AUTHENTICATED' || session.userId == null) {
    return (
      <div className="space-y-4">
        <p className="text-sm text-gray-600">피드를 작성하거나 수정하려면 로그인이 필요합니다.</p>
        <a className={getButtonStyles({})} href={getGithubLoginUrl()}>
          GitHub 로그인
        </a>
      </div>
    );
  }
  return props.feedId === undefined ? (
    <FeedForm
      key={session.userId}
      userId={session.userId}
      feedType={props.feedType ?? 'POST'}
      onCancel={props.onCancel}
      onSaved={props.onSaved}
    />
  ) : (
    <EditForm
      key={`${session.userId}-${props.feedId}`}
      userId={session.userId}
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
}: Omit<Required<FeedEditorPageProps>, 'feedType'> & { userId: number }) {
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
