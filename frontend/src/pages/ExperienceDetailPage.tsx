import { isHTTPError } from 'ky';

import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Button } from '@/components/Button';
import { Footer } from '@/components/Footer';
import { ExperienceDetail } from '@/components/projects/detail/ExperienceDetail';
import { ProjectNotApprovedError } from '@/errors/project';

/** 경험 상세(포트폴리오) 페이지. 프로젝트 상세와 같은 머리·꼬리를 쓴다. */
export function ExperienceDetailPage({ slug, cardId }: { slug: string; cardId: number }) {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <AppGnb />
      {/* 프로토타입 본문 너비 816px */}
      <main className="mx-auto w-full max-w-204 flex-1 px-4 pt-10 pb-24 md:px-6">
        <AsyncBoundary
          key={`${slug}/${cardId}`}
          fallback={
            <p role="status" className="py-16 text-center text-gray-500">
              경험을 불러오는 중입니다.
            </p>
          }
          errorFallback={(error, reset) => {
            const unavailable =
              error instanceof ProjectNotApprovedError ||
              (isHTTPError(error) && error.response.status === 404);
            return (
              <div role="alert" className="space-y-4 py-16 text-center">
                <title>경험 조회 오류 | shout-outz</title>
                <h1 className="text-xl font-bold">
                  {unavailable
                    ? '프로젝트가 없거나 접근할 수 없습니다.'
                    : '경험을 불러오지 못했습니다.'}
                </h1>
                {!unavailable && (
                  <Button variant="outline" onClick={reset}>
                    다시 시도
                  </Button>
                )}
              </div>
            );
          }}
        >
          <ExperienceDetail slug={slug} cardId={cardId} />
        </AsyncBoundary>
      </main>
      <Footer />
    </div>
  );
}
