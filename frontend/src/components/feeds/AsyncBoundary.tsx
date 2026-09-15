import { Component, Suspense, type ReactNode } from 'react';
import { QueryErrorResetBoundary } from '@tanstack/react-query';
import { Button } from '@/components/Button';
class Boundary extends Component<{ children: ReactNode; reset: () => void }, { failed: boolean }> {
  state = { failed: false };
  static getDerivedStateFromError() {
    return { failed: true };
  }
  render() {
    return this.state.failed ? (
      <div role="alert" className="rounded-xl border border-gray-200 p-6 text-gray-700">
        <p>불러오지 못했습니다. 다시 시도해 주세요.</p>
        <div className="mt-3">
          <Button
            variant="outline"
            onClick={() => {
              this.props.reset();
              this.setState({ failed: false });
            }}
          >
            다시 시도
          </Button>
        </div>
      </div>
    ) : (
      this.props.children
    );
  }
}
export function AsyncBoundary({ children }: { children: ReactNode }) {
  return (
    <QueryErrorResetBoundary>
      {({ reset }) => (
        <Boundary reset={reset}>
          <Suspense
            fallback={
              <p role="status" className="p-6 text-gray-500">
                불러오는 중…
              </p>
            }
          >
            {children}
          </Suspense>
        </Boundary>
      )}
    </QueryErrorResetBoundary>
  );
}
