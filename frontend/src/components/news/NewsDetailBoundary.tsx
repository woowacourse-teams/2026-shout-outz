import { Component, Suspense, type ReactNode } from 'react';
import { QueryErrorResetBoundary } from '@tanstack/react-query';
import { isHTTPError } from 'ky';

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

    const unavailable = isHTTPError(this.state.error) && this.state.error.response.status === 404;

    return (
      <div role="alert" className="space-y-4 py-16 text-center">
        <title>소식 조회 오류 | shout-outz</title>
        <h1 className="text-xl font-bold">
          {unavailable ? '소식이 없거나 접근할 수 없습니다.' : '소식을 불러오지 못했습니다.'}
        </h1>
        {!unavailable && (
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

export function NewsDetailBoundary({ children }: { children: ReactNode }) {
  return (
    <QueryErrorResetBoundary>
      {({ reset }) => (
        <DetailErrorBoundary onReset={reset}>
          <Suspense
            fallback={
              <p role="status" className="py-16 text-center text-sm text-gray-500">
                소식을 불러오는 중…
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
