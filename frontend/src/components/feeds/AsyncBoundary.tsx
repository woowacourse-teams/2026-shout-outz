import { Component, Suspense, type ReactNode } from 'react';
import { QueryErrorResetBoundary } from '@tanstack/react-query';
import { Button } from '@/components/Button';
import { getApiErrorMessage } from '@/utils/error';

class Boundary extends Component<
  { children: ReactNode; reset: () => void },
  { error: unknown | null }
> {
  state: { error: unknown | null } = { error: null };

  static getDerivedStateFromError(error: unknown) {
    return { error };
  }

  render() {
    return this.state.error !== null ? (
      <div role="alert" className="rounded-xl border border-gray-200 p-6 text-gray-700">
        <p>{getApiErrorMessage(this.state.error)}</p>
        <div className="mt-3">
          <Button
            variant="outline"
            onClick={() => {
              this.props.reset();
              this.setState({ error: null });
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
