import { Component, Suspense, type ReactNode } from 'react';
import { QueryErrorResetBoundary } from '@tanstack/react-query';
import { Button } from '@/components/Button';

class ListErrorBoundary extends Component<
  { children: ReactNode; onReset: () => void },
  { failed: boolean }
> {
  state = { failed: false };

  static getDerivedStateFromError() {
    return { failed: true };
  }

  render() {
    if (this.state.failed) {
      return (
        <div role="alert" className="space-y-4 py-16 text-center">
          <p className="text-gray-600">프로젝트를 불러오지 못했습니다.</p>
          <Button
            variant="outline"
            onClick={() => {
              this.props.onReset();
              this.setState({ failed: false });
            }}
          >
            다시 시도
          </Button>
        </div>
      );
    }
    return this.props.children;
  }
}

export function ProjectListBoundary({ children }: { children: ReactNode }) {
  return (
    <QueryErrorResetBoundary>
      {({ reset }) => (
        <ListErrorBoundary onReset={reset}>
          <Suspense
            fallback={
              <p role="status" className="py-16 text-center text-gray-500">
                프로젝트를 불러오는 중입니다.
              </p>
            }
          >
            {children}
          </Suspense>
        </ListErrorBoundary>
      )}
    </QueryErrorResetBoundary>
  );
}
