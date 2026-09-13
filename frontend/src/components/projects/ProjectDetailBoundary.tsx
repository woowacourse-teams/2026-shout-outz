import { Component, Suspense, type ReactNode } from 'react';
import { QueryErrorResetBoundary } from '@tanstack/react-query';
import { ProjectNotFoundError } from '@/api/project-detail';
import { Button } from '@/components/Button';

class DetailErrorBoundary extends Component<
  { children: ReactNode; onReset: () => void },
  { error: Error | null }
> {
  state: { error: Error | null } = { error: null };

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  render() {
    if (!this.state.error) return this.props.children;
    const notFound = this.state.error instanceof ProjectNotFoundError;
    return (
      <div role="alert" className="space-y-4 py-16 text-center">
        <title>프로젝트 조회 오류 | shout-outz</title>
        <h1 className="text-xl font-bold">
          {notFound ? '프로젝트가 없거나 접근할 수 없습니다.' : '프로젝트를 불러오지 못했습니다.'}
        </h1>
        {!notFound && (
          <Button
            variant="outline"
            onClick={() => {
              this.props.onReset();
              this.setState({ error: null });
            }}
          >
            다시 시도
          </Button>
        )}
      </div>
    );
  }
}

export function ProjectDetailBoundary({ children }: { children: ReactNode }) {
  return (
    <QueryErrorResetBoundary>
      {({ reset }) => (
        <DetailErrorBoundary onReset={reset}>
          <Suspense
            fallback={
              <p role="status" className="py-16 text-center text-gray-500">
                프로젝트 정보를 불러오는 중입니다.
              </p>
            }
          >
            {children}
          </Suspense>
        </DetailErrorBoundary>
      )}
    </QueryErrorResetBoundary>
  );
}
