import { Title } from '@/components/Title';
import { Footer } from '@/components/Footer';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Button } from '@/components/Button';
import {
  ProjectDetailContent,
  type ProjectDetailTab,
} from '@/components/projects/ProjectDetailContent';
import { ProjectNotApprovedError } from '@/errors/project';
import { isHTTPError } from 'ky';

interface ProjectDetailPageProps {
  slug: string;
  tab: ProjectDetailTab;
  onTabChange: (tab: ProjectDetailTab) => void;
}

export function ProjectDetailPage({ slug, tab, onTabChange }: ProjectDetailPageProps) {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <AppGnb />
      <main className="mx-auto w-full max-w-240 flex-1 px-4 pt-10 pb-24 md:px-6 md:pt-14">
        <AsyncBoundary
          key={slug}
          fallback={
            <p role="status" className="py-16 text-center text-gray-500">
              프로젝트 정보를 불러오는 중입니다.
            </p>
          }
          errorFallback={(error, reset) => {
            const unavailable =
              error instanceof ProjectNotApprovedError ||
              (isHTTPError(error) && error.response.status === 404);
            return (
              <div role="alert" className="space-y-4 py-16 text-center">
                <Title>프로젝트 조회 오류 | shout-outz</Title>
                <h1 className="text-xl font-bold">
                  {unavailable
                    ? '프로젝트가 없거나 접근할 수 없습니다.'
                    : '프로젝트를 불러오지 못했습니다.'}
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
          <ProjectDetailContent slug={slug} tab={tab} onTabChange={onTabChange} />
        </AsyncBoundary>
      </main>
      <Footer />
    </div>
  );
}
