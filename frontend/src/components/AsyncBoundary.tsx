import { Component, Suspense, type ReactNode } from 'react';
import { QueryErrorResetBoundary } from '@tanstack/react-query';

import { Button } from '@/components/Button';
import { getApiErrorMessage } from '@/utils/error';

type ErrorFallback = (error: unknown, reset: () => void) => ReactNode;

class Boundary extends Component<
  { children: ReactNode; reset: () => void; errorFallback?: ErrorFallback },
  { error: unknown | null }
> {
  state: { error: unknown | null } = { error: null };

  static getDerivedStateFromError(error: unknown) {
    return { error };
  }

  private reset = () => {
    this.props.reset();
    this.setState({ error: null });
  };

  render() {
    if (this.state.error === null) return this.props.children;
    if (this.props.errorFallback) return this.props.errorFallback(this.state.error, this.reset);

    return (
      <div role="alert" className="rounded-xl border border-gray-200 p-6 text-gray-700">
        <p>{getApiErrorMessage(this.state.error)}</p>
        <div className="mt-3">
          <Button variant="outline" onClick={this.reset}>
            다시 시도
          </Button>
        </div>
      </div>
    );
  }
}

export function AsyncBoundary({
  children,
  fallback,
  errorFallback,
}: {
  children: ReactNode;
  fallback?: ReactNode;
  errorFallback?: ErrorFallback;
}) {
  return (
    <QueryErrorResetBoundary>
      {({ reset }) => (
        <Boundary reset={reset} errorFallback={errorFallback}>
          <Suspense
            fallback={
              fallback ?? (
                <p role="status" className="p-6 text-gray-500">
                  불러오는 중…
                </p>
              )
            }
          >
            {children}
          </Suspense>
        </Boundary>
      )}
    </QueryErrorResetBoundary>
  );
}
