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
    if (!this.state.failed) return this.props.children;

    return (
      <div role="alert" className="space-y-4 py-16 text-center">
        <p className="text-gray-600">소식을 불러오지 못했습니다.</p>
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
}

export function NewsListBoundary({ children }: { children: ReactNode }) {
  return (
    <QueryErrorResetBoundary>
      {({ reset }) => (
        <ListErrorBoundary onReset={reset}>
          <Suspense
            fallback={
              <p role="status" className="py-16 text-center text-sm text-gray-500">
                소식을 불러오는 중…
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
